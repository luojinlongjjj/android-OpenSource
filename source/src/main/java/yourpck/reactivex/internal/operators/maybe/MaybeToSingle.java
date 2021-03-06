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

import java.util.NoSuchElementException;

import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.fuseable.HasUpstreamMaybeSource;

/**
 * Wraps a MaybeSource and exposes its onSuccess and onError signals and signals
 * NoSuchElementException for onComplete.
 *
 * @param <T> the value type
 */
public final class MaybeToSingle<T> extends Single<T> implements HasUpstreamMaybeSource<T> {

    final MaybeSource<T> source;
    final T defaultValue;

    public MaybeToSingle(MaybeSource<T> source, T defaultValue) {
        this.source = source;
        this.defaultValue = defaultValue;
    }

    @Override
    public MaybeSource<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        source.subscribe(new ToSingleMaybeSubscriber<T>(observer, defaultValue));
    }

    static final class ToSingleMaybeSubscriber<T> implements MaybeObserver<T>, Disposable {
        final SingleObserver<? super T> downstream;
        final T defaultValue;

        Disposable upstream;

        ToSingleMaybeSubscriber(SingleObserver<? super T> actual, T defaultValue) {
            this.downstream = actual;
            this.defaultValue = defaultValue;
        }

        @Override
        public void dispose() {
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            if (defaultValue != null) {
                downstream.onSuccess(defaultValue);
            } else {
                downstream.onError(new NoSuchElementException("The MaybeSource is empty"));
            }
        }
    }
}
