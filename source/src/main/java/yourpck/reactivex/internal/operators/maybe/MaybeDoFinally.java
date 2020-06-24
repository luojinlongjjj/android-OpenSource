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

import java.util.concurrent.atomic.AtomicInteger;

import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Action;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Execute an action after an onSuccess, onError, onComplete or a dispose event.
 * <p>History: 2.0.1 - experimental
 *
 * @param <T> the value type
 * @since 2.1
 */
public final class MaybeDoFinally<T> extends AbstractMaybeWithUpstream<T, T> {

    final Action onFinally;

    public MaybeDoFinally(MaybeSource<T> source, Action onFinally) {
        super(source);
        this.onFinally = onFinally;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new DoFinallyObserver<T>(observer, onFinally));
    }

    static final class DoFinallyObserver<T> extends AtomicInteger implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = 4109457741734051389L;

        final MaybeObserver<? super T> downstream;

        final Action onFinally;

        Disposable upstream;

        DoFinallyObserver(MaybeObserver<? super T> actual, Action onFinally) {
            this.downstream = actual;
            this.onFinally = onFinally;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T t) {
            downstream.onSuccess(t);
            runFinally();
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
            runFinally();
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
            runFinally();
        }

        @Override
        public void dispose() {
            upstream.dispose();
            runFinally();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        void runFinally() {
            if (compareAndSet(0, 1)) {
                try {
                    onFinally.run();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.onError(ex);
                }
            }
        }
    }
}
