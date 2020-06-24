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

import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Calls a consumer after pushing the current item to the downstream.
 * <p>History: 2.0.1 - experimental
 *
 * @param <T> the value type
 * @since 2.1
 */
public final class MaybeDoAfterSuccess<T> extends AbstractMaybeWithUpstream<T, T> {

    final Consumer<? super T> onAfterSuccess;

    public MaybeDoAfterSuccess(MaybeSource<T> source, Consumer<? super T> onAfterSuccess) {
        super(source);
        this.onAfterSuccess = onAfterSuccess;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new DoAfterObserver<T>(observer, onAfterSuccess));
    }

    static final class DoAfterObserver<T> implements MaybeObserver<T>, Disposable {

        final MaybeObserver<? super T> downstream;

        final Consumer<? super T> onAfterSuccess;

        Disposable upstream;

        DoAfterObserver(MaybeObserver<? super T> actual, Consumer<? super T> onAfterSuccess) {
            this.downstream = actual;
            this.onAfterSuccess = onAfterSuccess;
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

            try {
                onAfterSuccess.accept(t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                // remember, onSuccess is a terminal event and we can't call onError
                RxJavaPlugins.onError(ex);
            }
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        @Override
        public void dispose() {
            upstream.dispose();
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }
    }
}
