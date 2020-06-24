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

import yourpck.reactivex.Observable;
import yourpck.reactivex.ObservableSource;
import yourpck.reactivex.Observer;
import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.fuseable.FuseToObservable;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class ObservableCountSingle<T> extends Single<Long> implements FuseToObservable<Long> {
    final ObservableSource<T> source;

    public ObservableCountSingle(ObservableSource<T> source) {
        this.source = source;
    }

    @Override
    public void subscribeActual(SingleObserver<? super Long> t) {
        source.subscribe(new CountObserver(t));
    }

    @Override
    public Observable<Long> fuseToObservable() {
        return RxJavaPlugins.onAssembly(new ObservableCount<T>(source));
    }

    static final class CountObserver implements Observer<Object>, Disposable {
        final SingleObserver<? super Long> downstream;

        Disposable upstream;

        long count;

        CountObserver(SingleObserver<? super Long> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(Object t) {
            count++;
        }

        @Override
        public void onError(Throwable t) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            downstream.onSuccess(count);
        }
    }
}
