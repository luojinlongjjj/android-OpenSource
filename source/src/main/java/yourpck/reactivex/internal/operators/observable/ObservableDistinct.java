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

import yourpck.reactivex.ObservableSource;
import yourpck.reactivex.Observer;
import yourpck.reactivex.annotations.Nullable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.observers.BasicFuseableObserver;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class ObservableDistinct<T, K> extends AbstractObservableWithUpstream<T, T> {

    final Function<? super T, K> keySelector;

    final Callable<? extends Collection<? super K>> collectionSupplier;

    public ObservableDistinct(ObservableSource<T> source, Function<? super T, K> keySelector, Callable<? extends Collection<? super K>> collectionSupplier) {
        super(source);
        this.keySelector = keySelector;
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        Collection<? super K> collection;

        try {
            collection = ObjectHelper.requireNonNull(collectionSupplier.call(), "The collectionSupplier returned a null collection. Null values are generally not allowed in 2.x operators and sources.");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        source.subscribe(new DistinctObserver<T, K>(observer, keySelector, collection));
    }

    static final class DistinctObserver<T, K> extends BasicFuseableObserver<T, T> {

        final Collection<? super K> collection;

        final Function<? super T, K> keySelector;

        DistinctObserver(Observer<? super T> actual, Function<? super T, K> keySelector, Collection<? super K> collection) {
            super(actual);
            this.keySelector = keySelector;
            this.collection = collection;
        }

        @Override
        public void onNext(T value) {
            if (done) {
                return;
            }
            if (sourceMode == NONE) {
                K key;
                boolean b;

                try {
                    key = ObjectHelper.requireNonNull(keySelector.apply(value), "The keySelector returned a null key");
                    b = collection.add(key);
                } catch (Throwable ex) {
                    fail(ex);
                    return;
                }

                if (b) {
                    downstream.onNext(value);
                }
            } else {
                downstream.onNext(null);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
            } else {
                done = true;
                collection.clear();
                downstream.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                collection.clear();
                downstream.onComplete();
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            for (; ; ) {
                T v = qd.poll();

                if (v == null || collection.add(ObjectHelper.requireNonNull(keySelector.apply(v), "The keySelector returned a null key"))) {
                    return v;
                }
            }
        }

        @Override
        public void clear() {
            collection.clear();
            super.clear();
        }
    }
}
