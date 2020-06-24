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
package yourpck.reactivex.internal.operators.single;

import yourpck.reactivex.Observable;
import yourpck.reactivex.Observer;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.observers.DeferredScalarDisposable;

/**
 * Wraps a Single and exposes it as an Observable.
 *
 * @param <T> the value type
 */
public final class SingleToObservable<T> extends Observable<T> {

    final SingleSource<? extends T> source;

    public SingleToObservable(SingleSource<? extends T> source) {
        this.source = source;
    }

    /**
     * Creates a {@link SingleObserver} wrapper around a {@link Observer}.
     * <p>History: 2.0.1 - experimental
     *
     * @param <T>        the value type
     * @param downstream the downstream {@code Observer} to talk to
     * @return the new SingleObserver instance
     * @since 2.2
     */
    public static <T> SingleObserver<T> create(Observer<? super T> downstream) {
        return new SingleToObservableObserver<T>(downstream);
    }

    @Override
    public void subscribeActual(final Observer<? super T> observer) {
        source.subscribe(create(observer));
    }

    static final class SingleToObservableObserver<T>
            extends DeferredScalarDisposable<T>
            implements SingleObserver<T> {

        private static final long serialVersionUID = 3786543492451018833L;
        Disposable upstream;

        SingleToObservableObserver(Observer<? super T> downstream) {
            super(downstream);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            complete(value);
        }

        @Override
        public void onError(Throwable e) {
            error(e);
        }

        @Override
        public void dispose() {
            super.dispose();
            upstream.dispose();
        }

    }
}
