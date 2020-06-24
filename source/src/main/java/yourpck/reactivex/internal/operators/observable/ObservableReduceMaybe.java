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

package yourpck.reactivex.internal.operators.observable;

import yourpck.reactivex.Maybe;
import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.ObservableSource;
import yourpck.reactivex.Observer;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.BiFunction;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Reduce a sequence of values into a single value via an aggregator function and emit the final value or complete
 * if the source is empty.
 *
 * @param <T> the source and result value type
 */
public final class ObservableReduceMaybe<T> extends Maybe<T> {

    final ObservableSource<T> source;

    final BiFunction<T, T, T> reducer;

    public ObservableReduceMaybe(ObservableSource<T> source, BiFunction<T, T, T> reducer) {
        this.source = source;
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new ReduceObserver<T>(observer, reducer));
    }

    static final class ReduceObserver<T> implements Observer<T>, Disposable {

        final MaybeObserver<? super T> downstream;

        final BiFunction<T, T, T> reducer;

        boolean done;

        T value;

        Disposable upstream;

        ReduceObserver(MaybeObserver<? super T> observer, BiFunction<T, T, T> reducer) {
            this.downstream = observer;
            this.reducer = reducer;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T value) {
            if (!done) {
                T v = this.value;

                if (v == null) {
                    this.value = value;
                } else {
                    try {
                        this.value = ObjectHelper.requireNonNull(reducer.apply(v, value), "The reducer returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        upstream.dispose();
                        onError(ex);
                    }
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            value = null;
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            T v = value;
            value = null;
            if (v != null) {
                downstream.onSuccess(v);
            } else {
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
