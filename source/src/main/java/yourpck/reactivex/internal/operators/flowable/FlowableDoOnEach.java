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

import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.annotations.Nullable;
import yourpck.reactivex.exceptions.CompositeException;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Action;
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.internal.fuseable.ConditionalSubscriber;
import yourpck.reactivex.internal.subscribers.BasicFuseableConditionalSubscriber;
import yourpck.reactivex.internal.subscribers.BasicFuseableSubscriber;
import yourpck.reactivex.internal.util.ExceptionHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class FlowableDoOnEach<T> extends AbstractFlowableWithUpstream<T, T> {
    final Consumer<? super T> onNext;
    final Consumer<? super Throwable> onError;
    final Action onComplete;
    final Action onAfterTerminate;

    public FlowableDoOnEach(Flowable<T> source, Consumer<? super T> onNext,
                            Consumer<? super Throwable> onError,
                            Action onComplete,
                            Action onAfterTerminate) {
        super(source);
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
        this.onAfterTerminate = onAfterTerminate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new DoOnEachConditionalSubscriber<T>(
                    (ConditionalSubscriber<? super T>) s, onNext, onError, onComplete, onAfterTerminate));
        } else {
            source.subscribe(new DoOnEachSubscriber<T>(
                    s, onNext, onError, onComplete, onAfterTerminate));
        }
    }

    static final class DoOnEachSubscriber<T> extends BasicFuseableSubscriber<T, T> {
        final Consumer<? super T> onNext;
        final Consumer<? super Throwable> onError;
        final Action onComplete;
        final Action onAfterTerminate;

        DoOnEachSubscriber(
                Subscriber<? super T> actual,
                Consumer<? super T> onNext,
                Consumer<? super Throwable> onError,
                Action onComplete,
                Action onAfterTerminate) {
            super(actual);
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onAfterTerminate = onAfterTerminate;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return;
            }

            try {
                onNext.accept(t);
            } catch (Throwable e) {
                fail(e);
                return;
            }

            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            boolean relay = true;
            try {
                onError.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(new CompositeException(t, e));
                relay = false;
            }
            if (relay) {
                downstream.onError(t);
            }

            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            try {
                onComplete.run();
            } catch (Throwable e) {
                fail(e);
                return;
            }

            done = true;
            downstream.onComplete();

            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            T v;

            try {
                v = qs.poll();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                try {
                    onError.accept(ex);
                } catch (Throwable exc) {
                    throw new CompositeException(ex, exc);
                }
                throw ExceptionHelper.<Exception>throwIfThrowable(ex);
            }

            if (v != null) {
                try {
                    try {
                        onNext.accept(v);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        try {
                            onError.accept(ex);
                        } catch (Throwable exc) {
                            throw new CompositeException(ex, exc);
                        }
                        throw ExceptionHelper.<Exception>throwIfThrowable(ex);
                    }
                } finally {
                    onAfterTerminate.run();
                }
            } else {
                if (sourceMode == SYNC) {
                    onComplete.run();

                    onAfterTerminate.run();
                }
            }
            return v;
        }
    }

    static final class DoOnEachConditionalSubscriber<T> extends BasicFuseableConditionalSubscriber<T, T> {
        final Consumer<? super T> onNext;
        final Consumer<? super Throwable> onError;
        final Action onComplete;
        final Action onAfterTerminate;

        DoOnEachConditionalSubscriber(
                ConditionalSubscriber<? super T> actual,
                Consumer<? super T> onNext,
                Consumer<? super Throwable> onError,
                Action onComplete,
                Action onAfterTerminate) {
            super(actual);
            this.onNext = onNext;
            this.onError = onError;
            this.onComplete = onComplete;
            this.onAfterTerminate = onAfterTerminate;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return;
            }

            try {
                onNext.accept(t);
            } catch (Throwable e) {
                fail(e);
                return;
            }

            downstream.onNext(t);
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }

            try {
                onNext.accept(t);
            } catch (Throwable e) {
                fail(e);
                return false;
            }

            return downstream.tryOnNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            boolean relay = true;
            try {
                onError.accept(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                downstream.onError(new CompositeException(t, e));
                relay = false;
            }
            if (relay) {
                downstream.onError(t);
            }

            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            try {
                onComplete.run();
            } catch (Throwable e) {
                fail(e);
                return;
            }

            done = true;
            downstream.onComplete();

            try {
                onAfterTerminate.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            T v;

            try {
                v = qs.poll();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                try {
                    onError.accept(ex);
                } catch (Throwable exc) {
                    throw new CompositeException(ex, exc);
                }
                throw ExceptionHelper.<Exception>throwIfThrowable(ex);
            }

            if (v != null) {
                try {
                    try {
                        onNext.accept(v);
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        try {
                            onError.accept(ex);
                        } catch (Throwable exc) {
                            throw new CompositeException(ex, exc);
                        }
                        throw ExceptionHelper.<Exception>throwIfThrowable(ex);
                    }
                } finally {
                    onAfterTerminate.run();
                }
            } else {
                if (sourceMode == SYNC) {
                    onComplete.run();

                    onAfterTerminate.run();
                }
            }
            return v;
        }
    }
}
