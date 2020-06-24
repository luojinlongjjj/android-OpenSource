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

import yourpck.reactivex.CompletableObserver;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.exceptions.OnErrorNotImplementedException;
import yourpck.reactivex.functions.Action;
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.observers.LambdaConsumerIntrospection;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class CallbackCompletableObserver
        extends AtomicReference<Disposable>
        implements CompletableObserver, Disposable, Consumer<Throwable>, LambdaConsumerIntrospection {

    private static final long serialVersionUID = -4361286194466301354L;

    final Consumer<? super Throwable> onError;
    final Action onComplete;

    public CallbackCompletableObserver(Action onComplete) {
        this.onError = this;
        this.onComplete = onComplete;
    }

    public CallbackCompletableObserver(Consumer<? super Throwable> onError, Action onComplete) {
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public void accept(Throwable e) {
        RxJavaPlugins.onError(new OnErrorNotImplementedException(e));
    }

    @Override
    public void onComplete() {
        try {
            onComplete.run();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
        lazySet(DisposableHelper.DISPOSED);
    }

    @Override
    public void onError(Throwable e) {
        try {
            onError.accept(e);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(ex);
        }
        lazySet(DisposableHelper.DISPOSED);
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
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
        return onError != this;
    }
}