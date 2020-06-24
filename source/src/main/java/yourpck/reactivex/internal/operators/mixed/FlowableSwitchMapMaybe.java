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

package yourpck.reactivex.internal.operators.mixed;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.internal.util.AtomicThrowable;
import yourpck.reactivex.internal.util.BackpressureHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Maps the upstream items into {@link MaybeSource}s and switches (subscribes) to the newer ones
 * while disposing the older ones and emits the latest success value if available, optionally delaying
 * errors from the main source or the inner sources.
 * <p>History: 2.1.11 - experimental
 *
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 * @since 2.2
 */
public final class FlowableSwitchMapMaybe<T, R> extends Flowable<R> {

    final Flowable<T> source;

    final Function<? super T, ? extends MaybeSource<? extends R>> mapper;

    final boolean delayErrors;

    public FlowableSwitchMapMaybe(Flowable<T> source,
                                  Function<? super T, ? extends MaybeSource<? extends R>> mapper,
                                  boolean delayErrors) {
        this.source = source;
        this.mapper = mapper;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new SwitchMapMaybeSubscriber<T, R>(s, mapper, delayErrors));
    }

    static final class SwitchMapMaybeSubscriber<T, R> extends AtomicInteger
            implements FlowableSubscriber<T>, Subscription {

        static final SwitchMapMaybeObserver<Object> INNER_DISPOSED =
                new SwitchMapMaybeObserver<Object>(null);
        private static final long serialVersionUID = -5402190102429853762L;
        final Subscriber<? super R> downstream;
        final Function<? super T, ? extends MaybeSource<? extends R>> mapper;
        final boolean delayErrors;
        final AtomicThrowable errors;
        final AtomicLong requested;
        final AtomicReference<SwitchMapMaybeObserver<R>> inner;
        Subscription upstream;

        volatile boolean done;

        volatile boolean cancelled;

        long emitted;

        SwitchMapMaybeSubscriber(Subscriber<? super R> downstream,
                                 Function<? super T, ? extends MaybeSource<? extends R>> mapper,
                                 boolean delayErrors) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.requested = new AtomicLong();
            this.inner = new AtomicReference<SwitchMapMaybeObserver<R>>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                upstream = s;
                downstream.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void onNext(T t) {
            SwitchMapMaybeObserver<R> current = inner.get();
            if (current != null) {
                current.dispose();
            }

            MaybeSource<? extends R> ms;

            try {
                ms = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                inner.getAndSet((SwitchMapMaybeObserver) INNER_DISPOSED);
                onError(ex);
                return;
            }

            SwitchMapMaybeObserver<R> observer = new SwitchMapMaybeObserver<R>(this);

            for (; ; ) {
                current = inner.get();
                if (current == INNER_DISPOSED) {
                    break;
                }
                if (inner.compareAndSet(current, observer)) {
                    ms.subscribe(observer);
                    break;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                if (!delayErrors) {
                    disposeInner();
                }
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            done = true;
            drain();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        void disposeInner() {
            SwitchMapMaybeObserver<R> current = inner.getAndSet((SwitchMapMaybeObserver) INNER_DISPOSED);
            if (current != null && current != INNER_DISPOSED) {
                current.dispose();
            }
        }

        @Override
        public void request(long n) {
            BackpressureHelper.add(requested, n);
            drain();
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstream.cancel();
            disposeInner();
        }

        void innerError(SwitchMapMaybeObserver<R> sender, Throwable ex) {
            if (inner.compareAndSet(sender, null)) {
                if (errors.addThrowable(ex)) {
                    if (!delayErrors) {
                        upstream.cancel();
                        disposeInner();
                    }
                    drain();
                    return;
                }
            }
            RxJavaPlugins.onError(ex);
        }

        void innerComplete(SwitchMapMaybeObserver<R> sender) {
            if (inner.compareAndSet(sender, null)) {
                drain();
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missed = 1;
            Subscriber<? super R> downstream = this.downstream;
            AtomicThrowable errors = this.errors;
            AtomicReference<SwitchMapMaybeObserver<R>> inner = this.inner;
            AtomicLong requested = this.requested;
            long emitted = this.emitted;

            for (; ; ) {

                for (; ; ) {
                    if (cancelled) {
                        return;
                    }

                    if (errors.get() != null) {
                        if (!delayErrors) {
                            Throwable ex = errors.terminate();
                            downstream.onError(ex);
                            return;
                        }
                    }

                    boolean d = done;
                    SwitchMapMaybeObserver<R> current = inner.get();
                    boolean empty = current == null;

                    if (d && empty) {
                        Throwable ex = errors.terminate();
                        if (ex != null) {
                            downstream.onError(ex);
                        } else {
                            downstream.onComplete();
                        }
                        return;
                    }

                    if (empty || current.item == null || emitted == requested.get()) {
                        break;
                    }

                    inner.compareAndSet(current, null);

                    downstream.onNext(current.item);

                    emitted++;
                }

                this.emitted = emitted;
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        static final class SwitchMapMaybeObserver<R>
                extends AtomicReference<Disposable> implements MaybeObserver<R> {

            private static final long serialVersionUID = 8042919737683345351L;

            final SwitchMapMaybeSubscriber<?, R> parent;

            volatile R item;

            SwitchMapMaybeObserver(SwitchMapMaybeSubscriber<?, R> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
            }

            @Override
            public void onSuccess(R t) {
                item = t;
                parent.drain();
            }

            @Override
            public void onError(Throwable e) {
                parent.innerError(this, e);
            }

            @Override
            public void onComplete() {
                parent.innerComplete(this);
            }

            void dispose() {
                DisposableHelper.dispose(this);
            }
        }
    }
}
