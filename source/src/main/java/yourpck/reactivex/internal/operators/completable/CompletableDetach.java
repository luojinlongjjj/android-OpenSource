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

package yourpck.reactivex.internal.operators.completable;

import yourpck.reactivex.Completable;
import yourpck.reactivex.CompletableObserver;
import yourpck.reactivex.CompletableSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;

/**
 * Breaks the references between the upstream and downstream when the Completable terminates.
 * <p>History: 2.1.5 - experimental
 *
 * @since 2.2
 */
public final class CompletableDetach extends Completable {

    final CompletableSource source;

    public CompletableDetach(CompletableSource source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(CompletableObserver observer) {
        source.subscribe(new DetachCompletableObserver(observer));
    }

    static final class DetachCompletableObserver implements CompletableObserver, Disposable {

        CompletableObserver downstream;

        Disposable upstream;

        DetachCompletableObserver(CompletableObserver downstream) {
            this.downstream = downstream;
        }

        @Override
        public void dispose() {
            downstream = null;
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
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            CompletableObserver a = downstream;
            if (a != null) {
                downstream = null;
                a.onError(e);
            }
        }

        @Override
        public void onComplete() {
            upstream = DisposableHelper.DISPOSED;
            CompletableObserver a = downstream;
            if (a != null) {
                downstream = null;
                a.onComplete();
            }
        }
    }
}
