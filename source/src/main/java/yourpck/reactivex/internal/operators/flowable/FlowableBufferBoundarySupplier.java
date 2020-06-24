/**
 * Copyright (c) 2016-present, RxJava Contributors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package yourpck.reactivex.internal.operators.flowable;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.org.reactivestreams.Publisher;
import yourpck.org.reactivestreams.Subscriber;
import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.queue.MpscLinkedQueue;
import yourpck.reactivex.internal.subscribers.QueueDrainSubscriber;
import yourpck.reactivex.internal.subscriptions.EmptySubscription;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.internal.util.QueueDrainHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;
import yourpck.reactivex.subscribers.DisposableSubscriber;
import yourpck.reactivex.subscribers.SerializedSubscriber;

public final class FlowableBufferBoundarySupplier<T, U extends Collection<? super T>, B>
        extends AbstractFlowableWithUpstream<T, U> {
    final Callable<? extends Publisher<B>> boundarySupplier;
    final Callable<U> bufferSupplier;

    public FlowableBufferBoundarySupplier(Flowable<T> source, Callable<? extends Publisher<B>> boundarySupplier, Callable<U> bufferSupplier) {
        super(source);
        this.boundarySupplier = boundarySupplier;
        this.bufferSupplier = bufferSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        source.subscribe(new BufferBoundarySupplierSubscriber<T, U, B>(new SerializedSubscriber<U>(s), bufferSupplier, boundarySupplier));
    }

    static final class BufferBoundarySupplierSubscriber<T, U extends Collection<? super T>, B>
            extends QueueDrainSubscriber<T, U, U> implements FlowableSubscriber<T>, Subscription, Disposable {

        final Callable<U> bufferSupplier;
        final Callable<? extends Publisher<B>> boundarySupplier;
        final AtomicReference<Disposable> other = new AtomicReference<Disposable>();
        Subscription upstream;
        U buffer;

        BufferBoundarySupplierSubscriber(Subscriber<? super U> actual, Callable<U> bufferSupplier,
                                         Callable<? extends Publisher<B>> boundarySupplier) {
            super(actual, new MpscLinkedQueue<U>());
            this.bufferSupplier = bufferSupplier;
            this.boundarySupplier = boundarySupplier;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (!SubscriptionHelper.validate(this.upstream, s)) {
                return;
            }
            this.upstream = s;

            Subscriber<? super U> actual = this.downstream;

            U b;

            try {
                b = ObjectHelper.requireNonNull(bufferSupplier.call(), "The buffer supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancelled = true;
                s.cancel();
                EmptySubscription.error(e, actual);
                return;
            }

            buffer = b;

            Publisher<B> boundary;

            try {
                boundary = ObjectHelper.requireNonNull(boundarySupplier.call(), "The boundary publisher supplied is null");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                cancelled = true;
                s.cancel();
                EmptySubscription.error(ex, actual);
                return;
            }

            BufferBoundarySubscriber<T, U, B> bs = new BufferBoundarySubscriber<T, U, B>(this);
            other.set(bs);

            actual.onSubscribe(this);

            if (!cancelled) {
                s.request(Long.MAX_VALUE);

                boundary.subscribe(bs);
            }
        }

        @Override
        public void onNext(T t) {
            synchronized (this) {
                U b = buffer;
                if (b == null) {
                    return;
                }
                b.add(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            cancel();
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            U b;
            synchronized (this) {
                b = buffer;
                if (b == null) {
                    return;
                }
                buffer = null;
            }
            queue.offer(b);
            done = true;
            if (enter()) {
                QueueDrainHelper.drainMaxLoop(queue, downstream, false, this, this);
            }
        }

        @Override
        public void request(long n) {
            requested(n);
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                upstream.cancel();
                disposeOther();

                if (enter()) {
                    queue.clear();
                }
            }
        }

        void disposeOther() {
            DisposableHelper.dispose(other);
        }

        void next() {

            U next;

            try {
                next = ObjectHelper.requireNonNull(bufferSupplier.call(), "The buffer supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                cancel();
                downstream.onError(e);
                return;
            }

            Publisher<B> boundary;

            try {
                boundary = ObjectHelper.requireNonNull(boundarySupplier.call(), "The boundary publisher supplied is null");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                cancelled = true;
                upstream.cancel();
                downstream.onError(ex);
                return;
            }

            BufferBoundarySubscriber<T, U, B> bs = new BufferBoundarySubscriber<T, U, B>(this);

            if (DisposableHelper.replace(other, bs)) {
                U b;
                synchronized (this) {
                    b = buffer;
                    if (b == null) {
                        return;
                    }
                    buffer = next;
                }

                boundary.subscribe(bs);

                fastPathEmitMax(b, false, this);
            }
        }

        @Override
        public void dispose() {
            upstream.cancel();
            disposeOther();
        }

        @Override
        public boolean isDisposed() {
            return other.get() == DisposableHelper.DISPOSED;
        }

        @Override
        public boolean accept(Subscriber<? super U> a, U v) {
            downstream.onNext(v);
            return true;
        }

    }

    static final class BufferBoundarySubscriber<T, U extends Collection<? super T>, B> extends DisposableSubscriber<B> {
        final BufferBoundarySupplierSubscriber<T, U, B> parent;

        boolean once;

        BufferBoundarySubscriber(BufferBoundarySupplierSubscriber<T, U, B> parent) {
            this.parent = parent;
        }

        @Override
        public void onNext(B t) {
            if (once) {
                return;
            }
            once = true;
            cancel();
            parent.next();
        }

        @Override
        public void onError(Throwable t) {
            if (once) {
                RxJavaPlugins.onError(t);
                return;
            }
            once = true;
            parent.onError(t);
        }

        @Override
        public void onComplete() {
            if (once) {
                return;
            }
            once = true;
            parent.next();
        }
    }
}
