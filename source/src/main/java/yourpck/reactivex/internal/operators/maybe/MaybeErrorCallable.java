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

import java.util.concurrent.Callable;

import yourpck.reactivex.Maybe;
import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.disposables.Disposables;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.functions.ObjectHelper;

/**
 * Signals a Throwable returned by a Callable.
 *
 * @param <T> the value type
 */
public final class MaybeErrorCallable<T> extends Maybe<T> {

    final Callable<? extends Throwable> errorSupplier;

    public MaybeErrorCallable(Callable<? extends Throwable> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        observer.onSubscribe(Disposables.disposed());
        Throwable ex;

        try {
            ex = ObjectHelper.requireNonNull(errorSupplier.call(), "Callable returned null throwable. Null values are generally not allowed in 2.x operators and sources.");
        } catch (Throwable ex1) {
            Exceptions.throwIfFatal(ex1);
            ex = ex1;
        }

        observer.onError(ex);
    }
}
