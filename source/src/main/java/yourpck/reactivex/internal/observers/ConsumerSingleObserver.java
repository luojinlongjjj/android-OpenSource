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

package yourpck.reactivex.internal.observers;

import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.CompositeException;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.Functions;
import yourpck.reactivex.observers.LambdaConsumerIntrospection;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class ConsumerSingleObserver<T>
        extends AtomicReference<Disposable>
        implements SingleObserver<T>, Disposable, LambdaConsumerIntrospection {

    private static final long serialVersionUID = -7012088219455310787L;

    final Consumer<? super T> onSuccess;

    final Consumer<? super Throwable> onError;

    public ConsumerSingleObserver(Consumer<? super T> onSuccess, Consumer<? super Throwable> onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    @Override
    public void onError(Throwable e) {
        lazySet(DisposableHelper.DISPOSED);
        try {
            onError.accept(e);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(new CompositeException(e, ex));
        }
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
    }

    @Override
    public void onSuccess(T value) {
        lazySet(DisposableHelper.DISPOSED);
        try {
            onSuccess.accept(value);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
    }

    @Override
    public void dispose() {
        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return get() == DisposableHelper.DISPOSED;
    }

    @Override
    public boolean hasCustomOnError() {
        return onError != Functions.ON_ERROR_MISSING;
    }
}
