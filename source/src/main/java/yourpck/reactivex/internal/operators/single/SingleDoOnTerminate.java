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

import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.CompositeException;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Action;

public final class SingleDoOnTerminate<T> extends Single<T> {

    final SingleSource<T> source;

    final Action onTerminate;

    public SingleDoOnTerminate(SingleSource<T> source, Action onTerminate) {
        this.source = source;
        this.onTerminate = onTerminate;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> observer) {
        source.subscribe(new DoOnTerminate(observer));
    }

    final class DoOnTerminate implements SingleObserver<T> {

        final SingleObserver<? super T> downstream;

        DoOnTerminate(SingleObserver<? super T> observer) {
            this.downstream = observer;
        }

        @Override
        public void onSubscribe(Disposable d) {
            downstream.onSubscribe(d);
        }

        @Override
        public void onSuccess(T value) {
            try {
                onTerminate.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            try {
                onTerminate.run();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                e = new CompositeException(e, ex);
            }

            downstream.onError(e);
        }
    }
}
