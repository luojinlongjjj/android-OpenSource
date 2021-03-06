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

package yourpck.reactivex.internal.operators.maybe;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;

/**
 * Maps the success value of the source MaybeSource into a Single.
 *
 * @param <T> the input value type
 * @param <R> the result value type
 */
public final class MaybeFlatMapSingle<T, R> extends Single<R> {

    final MaybeSource<T> source;

    final Function<? super T, ? extends SingleSource<? extends R>> mapper;

    public MaybeFlatMapSingle(MaybeSource<T> source, Function<? super T, ? extends SingleSource<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super R> downstream) {
        source.subscribe(new FlatMapMaybeObserver<T, R>(downstream, mapper));
    }

    static final class FlatMapMaybeObserver<T, R>
            extends AtomicReference<Disposable>
            implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = 4827726964688405508L;

        final SingleObserver<? super R> downstream;

        final Function<? super T, ? extends SingleSource<? extends R>> mapper;

        FlatMapMaybeObserver(SingleObserver<? super R> actual, Function<? super T, ? extends SingleSource<? extends R>> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            SingleSource<? extends R> ss;

            try {
                ss = ObjectHelper.requireNonNull(mapper.apply(value), "The mapper returned a null SingleSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                onError(ex);
                return;
            }

            if (!isDisposed()) {
                ss.subscribe(new FlatMapSingleObserver<R>(this, downstream));
            }
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onError(new NoSuchElementException());
        }
    }

    static final class FlatMapSingleObserver<R> implements SingleObserver<R> {

        final AtomicReference<Disposable> parent;

        final SingleObserver<? super R> downstream;

        FlatMapSingleObserver(AtomicReference<Disposable> parent, SingleObserver<? super R> downstream) {
            this.parent = parent;
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(final Disposable d) {
            DisposableHelper.replace(parent, d);
        }

        @Override
        public void onSuccess(final R value) {
            downstream.onSuccess(value);
        }

        @Override
        public void onError(final Throwable e) {
            downstream.onError(e);
        }
    }
}
