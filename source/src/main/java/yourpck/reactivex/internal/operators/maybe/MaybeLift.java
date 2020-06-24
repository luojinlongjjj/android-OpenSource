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

import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeOperator;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.internal.functions.ObjectHelper;

/**
 * Calls a MaybeOperator for the incoming MaybeObserver.
 *
 * @param <T> the upstream value type
 * @param <R> the downstream value type
 */
public final class MaybeLift<T, R> extends AbstractMaybeWithUpstream<T, R> {

    final MaybeOperator<? extends R, ? super T> operator;

    public MaybeLift(MaybeSource<T> source, MaybeOperator<? extends R, ? super T> operator) {
        super(source);
        this.operator = operator;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super R> observer) {
        MaybeObserver<? super T> lifted;

        try {
            lifted = ObjectHelper.requireNonNull(operator.apply(observer), "The operator returned a null MaybeObserver");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptyDisposable.error(ex, observer);
            return;
        }

        source.subscribe(lifted);
    }
}
