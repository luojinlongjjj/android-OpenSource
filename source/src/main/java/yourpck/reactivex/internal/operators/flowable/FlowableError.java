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

package yourpck.reactivex.internal.operators.flowable;

import java.util.concurrent.Callable;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.subscriptions.EmptySubscription;

public final class FlowableError<T> extends Flowable<T> {
    final Callable<? extends Throwable> errorSupplier;

    public FlowableError(Callable<? extends Throwable> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    @Override
    public void subscribeActual(Subscriber<? super T> s) {
        Throwable error;
        try {
            error = ObjectHelper.requireNonNull(errorSupplier.call(), "Callable returned null throwable. Null values are generally not allowed in 2.x operators and sources.");
        } catch (Throwable t) {
            Exceptions.throwIfFatal(t);
            error = t;
        }
        EmptySubscription.error(error, s);
    }
}
