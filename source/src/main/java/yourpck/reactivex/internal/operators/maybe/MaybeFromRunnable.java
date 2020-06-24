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
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.disposables.Disposables;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Executes an Runnable and signals its exception or completes normally.
 *
 * @param <T> the value type
 */
public final class MaybeFromRunnable<T> extends Maybe<T> implements Callable<T> {

    final Runnable runnable;

    public MaybeFromRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        Disposable d = Disposables.empty();
        observer.onSubscribe(d);

        if (!d.isDisposed()) {

            try {
                runnable.run();
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
                observer.onComplete();
            }
        }
    }

    @Override
    public T call() throws Exception {
        runnable.run();
        return null;
    }
}
