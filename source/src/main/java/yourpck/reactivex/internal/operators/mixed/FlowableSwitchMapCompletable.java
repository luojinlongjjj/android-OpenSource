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

import java.util.concurrent.atomic.AtomicReference;

import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.Completable;
import yourpck.reactivex.CompletableObserver;
import yourpck.reactivex.CompletableSource;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.internal.util.AtomicThrowable;
import yourpck.reactivex.internal.util.ExceptionHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Maps the upstream values into {@link CompletableSource}s, subscribes to the newer one while
 * disposing the subscription to the previous {@code CompletableSource}, thus keeping at most one
 * active {@code CompletableSource} running.
 * <p>History: 2.1.11 - experimental
 *
 * @param <T> the upstream value type
 * @since 2.2
 */
public final class FlowableSwitchMapCompletable<T> extends Completable {

    final Flowable<T> source;

    final Function<? super T, ? extends CompletableSource> mapper;

    final boolean delayErrors;

    public FlowableSwitchMapCompletable(Flowable<T> source,
                                        Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors) {
        this.source = source;
        this.mapper = mapper;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new SwitchMapCompletableObserver<T>(observer, mapper, delayErrors));
    }

    static final class SwitchMapCompletableObserver<T> implements FlowableSubscriber<T>, Disposable {

        static final SwitchMapInnerObserver INNER_DISPOSED = new SwitchMapInnerObserver(null);
        final CompletableObserver downstream;
        final Function<? super T, ? extends CompletableSource> mapper;
        final boolean delayErrors;
        final AtomicThrowable errors;
        final AtomicReference<SwitchMapInnerObserver> inner;
        volatile boolean done;

        Subscription upstream;

        SwitchMapCompletableObserver(CompletableObserver downstream,
                                     Function<? super T, ? extends CompletableSource> mapper, boolean delayErrors) {
            this.downstream = downstream;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.errors = new AtomicThrowable();
            this.inner = new AtomicReference<SwitchMapInnerObserver>();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            CompletableSource c;

            try {
                c = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null CompletableSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                upstream.cancel();
                onError(ex);
                return;
            }

            SwitchMapInnerObserver o = new SwitchMapInnerObserver(this);

            for (; ; ) {
                SwitchMapInnerObserver current = inner.get();
                if (current == INNER_DISPOSED) {
                    break;
                }
                if (inner.compareAndSet(current, o)) {
                    if (current != null) {
                        current.dispose();
                    }
                    c.subscribe(o);
                    break;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (errors.addThrowable(t)) {
                if (delayErrors) {
                    onComplete();
                } else {
                    disposeInner();
                    Throwable ex = errors.terminate();
                    if (ex != ExceptionHelper.TERMINATED) {
                        downstream.onError(ex);
                    }
                }
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            done = true;
            if (inner.get() == null) {
                Throwable ex = errors.terminate();
                if (ex == null) {
                    downstream.onComplete();
                } else {
                    downstream.onError(ex);
                }
            }
        }

        void disposeInner() {
            SwitchMapInnerObserver o = inner.getAndSet(INNER_DISPOSED);
            if (o != null && o != INNER_DISPOSED) {
                o.dispose();
            }
        }

        @Override
        public void dispose() {
            upstream.cancel();
            disposeInner();
        }

        @Override
        public boolean isDisposed() {
            return inner.get() == INNER_DISPOSED;
        }

        void innerError(SwitchMapInnerObserver sender, Throwable error) {
            if (inner.compareAndSet(sender, null)) {
                if (errors.addThrowable(error)) {
                    if (delayErrors) {
                        if (done) {
                            Throwable ex = errors.terminate();
                            downstream.onError(ex);
                        }
                    } else {
                        dispose();
                        Throwable ex = errors.terminate();
                        if (ex != ExceptionHelper.TERMINATED) {
                            downstream.onError(ex);
                        }
                    }
                    return;
                }
            }
            RxJavaPlugins.onError(error);
        }

        void innerComplete(SwitchMapInnerObserver sender) {
            if (inner.compareAndSet(sender, null)) {
                if (done) {
                    Throwable ex = errors.terminate();
                    if (ex == null) {
                        downstream.onComplete();
                    } else {
                        downstream.onError(ex);
                    }
                }
            }
        }

        static final class SwitchMapInnerObserver extends AtomicReference<Disposable>
                implements CompletableObserver {

            private static final long serialVersionUID = -8003404460084760287L;

            final SwitchMapCompletableObserver<?> parent;

            SwitchMapInnerObserver(SwitchMapCompletableObserver<?> parent) {
                this.parent = parent;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this, d);
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
