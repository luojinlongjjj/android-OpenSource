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
import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.fuseable.HasUpstreamMaybeSource;

/**
 * Signals true if the source signals a value that is object-equals with the provided
 * value, false otherwise or for empty sources.
 *
 * @param <T> the value type
 */
public final class MaybeContains<T> extends Single<Boolean> implements HasUpstreamMaybeSource<T> {

    final MaybeSource<T> source;

    final Object value;

    public MaybeContains(MaybeSource<T> source, Object value) {
        this.source = source;
        this.value = value;
    }

    @Override
    public MaybeSource<T> source() {
        return source;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super Boolean> observer) {
        source.subscribe(new ContainsMaybeObserver(observer, value));
    }

    static final class ContainsMaybeObserver implements MaybeObserver<Object>, Disposable {

        final SingleObserver<? super Boolean> downstream;

        final Object value;

        Disposable upstream;

        ContainsMaybeObserver(SingleObserver<? super Boolean> actual, Object value) {
            this.downstream = actual;
            this.value = value;
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
        public void onSuccess(Object value) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onSuccess(ObjectHelper.equals(value, this.value));
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            downstream.onSuccess(false);
        }
    }
}
