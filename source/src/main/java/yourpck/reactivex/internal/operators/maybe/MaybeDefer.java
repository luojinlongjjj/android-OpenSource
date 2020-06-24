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
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.internal.functions.ObjectHelper;

/**
 * Defers the creation of the actual Maybe the incoming MaybeObserver is subscribed to.
 *
 * @param <T> the value type
 */
public final class MaybeDefer<T> extends Maybe<T> {

    final Callable<? extends MaybeSource<? extends T>> maybeSupplier;

    public MaybeDefer(Callable<? extends MaybeSource<? extends T>> maybeSupplier) {
        this.maybeSupplier = maybeSupplier;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        MaybeSource<? extends T> source;

        try {
            source = ObjectHelper.requireNonNull(maybeSupplier.call(), "The maybeSupplier returned a null MaybeSource");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        source.subscribe(observer);
    }
}
