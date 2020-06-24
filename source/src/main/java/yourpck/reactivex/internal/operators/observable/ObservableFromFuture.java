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

package yourpck.reactivex.internal.operators.observable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import yourpck.reactivex.Observable;
import yourpck.reactivex.Observer;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.observers.DeferredScalarDisposable;

public final class ObservableFromFuture<T> extends Observable<T> {
    final Future<? extends T> future;
    final long timeout;
    final TimeUnit unit;

    public ObservableFromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        this.future = future;
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public void subscribeActual(Observer<? super T> observer) {
        DeferredScalarDisposable<T> d = new DeferredScalarDisposable<T>(observer);
        observer.onSubscribe(d);
        if (!d.isDisposed()) {
            T v;
            try {
                v = ObjectHelper.requireNonNull(unit != null ? future.get(timeout, unit) : future.get(), "Future returned null");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                if (!d.isDisposed()) {
                    observer.onError(ex);
                }
                return;
            }
            d.complete(v);
        }
    }
}
