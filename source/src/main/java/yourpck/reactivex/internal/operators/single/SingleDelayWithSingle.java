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

package yourpck.reactivex.internal.operators.single;

import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.observers.ResumeSingleObserver;

public final class SingleDelayWithSingle<T, U> extends Single<T> {

    final SingleSource<T> source;

    final SingleSource<U> other;

    public SingleDelayWithSingle(SingleSource<T> source, SingleSource<U> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super T> observer) {
        other.subscribe(new OtherObserver<T, U>(observer, source));
    }

    static final class OtherObserver<T, U>
            extends AtomicReference<Disposable>
            implements SingleObserver<U>, Disposable {

        private static final long serialVersionUID = -8565274649390031272L;

        final SingleObserver<? super T> downstream;

        final SingleSource<T> source;

        OtherObserver(SingleObserver<? super T> actual, SingleSource<T> source) {
            this.downstream = actual;
            this.source = source;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(U value) {
            source.subscribe(new ResumeSingleObserver<T>(this, downstream));
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }
}
