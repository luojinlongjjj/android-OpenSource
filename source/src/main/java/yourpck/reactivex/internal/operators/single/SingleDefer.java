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

import java.util.concurrent.Callable;

import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.internal.functions.ObjectHelper;

public final class SingleDefer<T> extends Single<T> {

    final Callable<? extends SingleSource<? extends T>> singleSupplier;

    public SingleDefer(Callable<? extends SingleSource<? extends T>> singleSupplier) {
        this.singleSupplier = singleSupplier;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        SingleSource<? extends T> next;

        try {
            next = ObjectHelper.requireNonNull(singleSupplier.call(), "The singleSupplier returned a null SingleSource");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, observer);
            return;
        }

        next.subscribe(observer);
    }

}
