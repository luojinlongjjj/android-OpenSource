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

import yourpck.reactivex.ObservableSource;
import yourpck.reactivex.Observer;
import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.BiFunction;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Reduce a sequence of values, starting from a seed value and by using
 * an accumulator function and return the last accumulated value.
 *
 * @param <T> the source value type
 * @param <R> the accumulated result type
 */
public final class ObservableReduceSeedSingle<T, R> extends Single<R> {

    final ObservableSource<T> source;

    final R seed;

    final BiFunction<R, ? super T, R> reducer;

    public ObservableReduceSeedSingle(ObservableSource<T> source, R seed, BiFunction<R, ? super T, R> reducer) {
        this.source = source;
        this.seed = seed;
        this.reducer = reducer;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> observer) {
        source.subscribe(new ReduceSeedObserver<T, R>(observer, reducer, seed));
    }

    static final class ReduceSeedObserver<T, R> implements Observer<T>, Disposable {

        final SingleObserver<? super R> downstream;

        final BiFunction<R, ? super T, R> reducer;

        R value;

        Disposable upstream;

        ReduceSeedObserver(SingleObserver<? super R> actual, BiFunction<R, ? super T, R> reducer, R value) {
            this.downstream = actual;
            this.value = value;
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
            R v = this.value;
            if (v != null) {
                try {
                    this.value = ObjectHelper.requireNonNull(reducer.apply(v, value), "The reducer returned a null value");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    upstream.dispose();
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            R v = value;
            if (v != null) {
                value = null;
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            R v = value;
            if (v != null) {
                value = null;
                downstream.onSuccess(v);
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
