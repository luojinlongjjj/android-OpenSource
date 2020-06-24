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
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Predicate;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.fuseable.FuseToObservable;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class ObservableAllSingle<T> extends Single<Boolean> implements FuseToObservable<Boolean> {
    final ObservableSource<T> source;

    final Predicate<? super T> predicate;

    public ObservableAllSingle(ObservableSource<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super Boolean> t) {
        source.subscribe(new AllObserver<T>(t, predicate));
    }

    @Override
    public Observable<Boolean> fuseToObservable() {
        return RxJavaPlugins.onAssembly(new ObservableAll<T>(source, predicate));
    }

    static final class AllObserver<T> implements Observer<T>, Disposable {
        final SingleObserver<? super Boolean> downstream;
        final Predicate<? super T> predicate;

        Disposable upstream;

        boolean done;

        AllObserver(SingleObserver<? super Boolean> actual, Predicate<? super T> predicate) {
            this.downstream = actual;
            this.predicate = predicate;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.dispose();
                onError(e);
                return;
            }
            if (!b) {
                done = true;
                upstream.dispose();
                downstream.onSuccess(false);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            downstream.onSuccess(true);
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
