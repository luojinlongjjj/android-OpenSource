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

import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.Maybe;
import yourpck.reactivex.MaybeEmitter;
import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeOnSubscribe;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Cancellable;
import yourpck.reactivex.internal.disposables.CancellableDisposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Provides an API over MaybeObserver that serializes calls to onXXX and manages cancellation
 * in a safe manner.
 *
 * @param <T> the value type emitted
 */
public final class MaybeCreate<T> extends Maybe<T> {

    final MaybeOnSubscribe<T> source;

    public MaybeCreate(MaybeOnSubscribe<T> source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        Emitter<T> parent = new Emitter<T>(observer);
        observer.onSubscribe(parent);

        try {
            source.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class Emitter<T>
            extends AtomicReference<Disposable>
            implements MaybeEmitter<T>, Disposable {

        private static final long serialVersionUID = -2467358622224974244L;
        final MaybeObserver<? super T> downstream;

        Emitter(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSuccess(T value) {
            if (get() != DisposableHelper.DISPOSED) {
                Disposable d = getAndSet(DisposableHelper.DISPOSED);
                if (d != DisposableHelper.DISPOSED) {
                    try {
                        if (value == null) {
                            downstream.onError(new NullPointerException("onSuccess called with null. Null values are generally not allowed in 2.x operators and sources."));
                        } else {
                            downstream.onSuccess(value);
                        }
                    } finally {
                        if (d != null) {
                            d.dispose();
                        }
                    }
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!tryOnError(t)) {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public boolean tryOnError(Throwable t) {
            if (t == null) {
                t = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
            }
            if (get() != DisposableHelper.DISPOSED) {
                Disposable d = getAndSet(DisposableHelper.DISPOSED);
                if (d != DisposableHelper.DISPOSED) {
                    try {
                        downstream.onError(t);
                    } finally {
                        if (d != null) {
                            d.dispose();
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onComplete() {
            if (get() != DisposableHelper.DISPOSED) {
                Disposable d = getAndSet(DisposableHelper.DISPOSED);
                if (d != DisposableHelper.DISPOSED) {
                    try {
                        downstream.onComplete();
                    } finally {
                        if (d != null) {
                            d.dispose();
                        }
                    }
                }
            }
        }

        @Override
        public void setDisposable(Disposable d) {
            DisposableHelper.set(this, d);
        }

        @Override
        public void setCancellable(Cancellable c) {
            setDisposable(new CancellableDisposable(c));
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public String toString() {
            return String.format("%s{%s}", getClass().getSimpleName(), super.toString());
        }
    }
}
