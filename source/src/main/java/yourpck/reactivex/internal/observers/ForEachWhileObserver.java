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

import yourpck.reactivex.Observer;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.CompositeException;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Action;
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.functions.Predicate;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class ForEachWhileObserver<T>
        extends AtomicReference<Disposable>
        implements Observer<T>, Disposable {

    private static final long serialVersionUID = -4403180040475402120L;

    final Predicate<? super T> onNext;

    final Consumer<? super Throwable> onError;

    final Action onComplete;

    boolean done;

    public ForEachWhileObserver(Predicate<? super T> onNext,
                                Consumer<? super Throwable> onError, Action onComplete) {
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this, d);
    }

    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }

        boolean b;
        try {
            b = onNext.test(t);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            dispose();
            onError(ex);
            return;
        }

        if (!b) {
            dispose();
            onComplete();
        }
    }

    @Override
    public void onError(Throwable t) {
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        done = true;
        try {
            onError.accept(t);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            RxJavaPlugins.onError(new CompositeException(t, ex));
        }
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        done = true;
        try {
            onComplete.run();
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
        return DisposableHelper.isDisposed(this.get());
    }
}
