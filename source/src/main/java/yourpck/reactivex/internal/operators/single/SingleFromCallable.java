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
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.disposables.Disposables;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class SingleFromCallable<T> extends Single<T> {

    final Callable<? extends T> callable;

    public SingleFromCallable(Callable<? extends T> callable) {
        this.callable = callable;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        Disposable d = Disposables.empty();
        observer.onSubscribe(d);

        if (d.isDisposed()) {
            return;
        }
        T value;

        try {
            value = ObjectHelper.requireNonNull(callable.call(), "The callable returned a null value");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            if (!d.isDisposed()) {
                observer.onError(ex);
            } else {
                RxJavaPlugins.onError(ex);
            }
            return;
        }

        if (!d.isDisposed()) {
            observer.onSuccess(value);
        }
    }
}
