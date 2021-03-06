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

import java.util.Collection;
import java.util.concurrent.Callable;

import yourpck.reactivex.Observable;
import yourpck.reactivex.ObservableSource;
import yourpck.reactivex.Observer;
import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.internal.functions.Functions;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.fuseable.FuseToObservable;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class ObservableToListSingle<T, U extends Collection<? super T>>
        extends Single<U> implements FuseToObservable<U> {

    final ObservableSource<T> source;

    final Callable<U> collectionSupplier;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ObservableToListSingle(ObservableSource<T> source, final int defaultCapacityHint) {
        this.source = source;
        this.collectionSupplier = (Callable) Functions.createArrayList(defaultCapacityHint);
    }

    public ObservableToListSingle(ObservableSource<T> source, Callable<U> collectionSupplier) {
        this.source = source;
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    public void subscribeActual(SingleObserver<? super U> t) {
        U coll;
        try {
            coll = ObjectHelper.requireNonNull(collectionSupplier.call(), "The collectionSupplier returned a null collection. Null values are generally not allowed in 2.x operators and sources.");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, t);
            return;
        }
        source.subscribe(new ToListObserver<T, U>(t, coll));
    }

    @Override
    public Observable<U> fuseToObservable() {
        return RxJavaPlugins.onAssembly(new ObservableToList<T, U>(source, collectionSupplier));
    }

    static final class ToListObserver<T, U extends Collection<? super T>> implements Observer<T>, Disposable {
        final SingleObserver<? super U> downstream;

        U collection;

        Disposable upstream;

        ToListObserver(SingleObserver<? super U> actual, U collection) {
            this.downstream = actual;
            this.collection = collection;
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
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onNext(T t) {
            collection.add(t);
        }

        @Override
        public void onError(Throwable t) {
            collection = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            U c = collection;
            collection = null;
            downstream.onSuccess(c);
        }
    }
}
