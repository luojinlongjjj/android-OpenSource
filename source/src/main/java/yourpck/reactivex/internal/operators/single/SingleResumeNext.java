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

import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.CompositeException;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.observers.ResumeSingleObserver;

public final class SingleResumeNext<T> extends Single<T> {
    final SingleSource<? extends T> source;

    final Function<? super Throwable, ? extends SingleSource<? extends T>> nextFunction;

    public SingleResumeNext(SingleSource<? extends T> source,
                            Function<? super Throwable, ? extends SingleSource<? extends T>> nextFunction) {
        this.source = source;
        this.nextFunction = nextFunction;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> observer) {
        source.subscribe(new ResumeMainSingleObserver<T>(observer, nextFunction));
    }

    static final class ResumeMainSingleObserver<T> extends AtomicReference<Disposable>
            implements SingleObserver<T>, Disposable {
        private static final long serialVersionUID = -5314538511045349925L;

        final SingleObserver<? super T> downstream;

        final Function<? super Throwable, ? extends SingleSource<? extends T>> nextFunction;

        ResumeMainSingleObserver(SingleObserver<? super T> actual,
                                 Function<? super Throwable, ? extends SingleSource<? extends T>> nextFunction) {
            this.downstream = actual;
            this.nextFunction = nextFunction;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            SingleSource<? extends T> source;

            try {
                source = ObjectHelper.requireNonNull(nextFunction.apply(e), "The nextFunction returned a null SingleSource.");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(e, ex));
                return;
            }

            source.subscribe(new ResumeSingleObserver<T>(this, downstream));
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }
}
