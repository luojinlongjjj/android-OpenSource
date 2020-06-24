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

import yourpck.reactivex.Observer;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Action;
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class DisposableLambdaObserver<T> implements Observer<T>, Disposable {
    final Observer<? super T> downstream;
    final Consumer<? super Disposable> onSubscribe;
    final Action onDispose;

    Disposable upstream;

    public DisposableLambdaObserver(Observer<? super T> actual,
                                    Consumer<? super Disposable> onSubscribe,
                                    Action onDispose) {
        this.downstream = actual;
        this.onSubscribe = onSubscribe;
        this.onDispose = onDispose;
    }

    @Override
    public void onSubscribe(Disposable d) {
        // this way, multiple calls to onSubscribe can show up in tests that use doOnSubscribe to validate behavior
        try {
            onSubscribe.accept(d);
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            d.dispose();
            this.upstream = DisposableHelper.DISPOSED;
            EmptyDisposable.error(e, downstream);
            return;
        }
        if (DisposableHelper.validate(this.upstream, d)) {
            this.upstream = d;
            downstream.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        downstream.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        if (upstream != DisposableHelper.DISPOSED) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(t);
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (upstream != DisposableHelper.DISPOSED) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onComplete();
        }
    }

    @Override
    public void dispose() {
        Disposable d = upstream;
        if (d != DisposableHelper.DISPOSED) {
            upstream = DisposableHelper.DISPOSED;
            try {
                onDispose.run();
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
            d.dispose();
        }
    }

    @Override
    public boolean isDisposed() {
        return upstream.isDisposed();
    }
}
