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

import yourpck.reactivex.Maybe;
import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.disposables.Disposables;
import yourpck.reactivex.internal.fuseable.ScalarCallable;

/**
 * Signals a constant value.
 *
 * @param <T> the value type
 */
public final class MaybeJust<T> extends Maybe<T> implements ScalarCallable<T> {

    final T value;

    public MaybeJust(T value) {
        this.value = value;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        observer.onSubscribe(Disposables.disposed());
        observer.onSuccess(value);
    }

    @Override
    public T call() {
        return value;
    }
}
