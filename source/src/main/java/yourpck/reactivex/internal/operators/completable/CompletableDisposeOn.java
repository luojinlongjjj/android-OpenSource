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
import yourpck.reactivex.Scheduler;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class CompletableDisposeOn extends Completable {

    final CompletableSource source;

    final Scheduler scheduler;

    public CompletableDisposeOn(CompletableSource source, Scheduler scheduler) {
        this.source = source;
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(final CompletableObserver observer) {
        source.subscribe(new DisposeOnObserver(observer, scheduler));
    }

    static final class DisposeOnObserver implements CompletableObserver, Disposable, Runnable {
        final CompletableObserver downstream;

        final Scheduler scheduler;

        Disposable upstream;

        volatile boolean disposed;

        DisposeOnObserver(CompletableObserver observer, Scheduler scheduler) {
            this.downstream = observer;
            this.scheduler = scheduler;
        }

        @Override
        public void onComplete() {
            if (disposed) {
                return;
            }
            downstream.onComplete();
        }

        @Override
        public void onError(Throwable e) {
            if (disposed) {
                RxJavaPlugins.onError(e);
                return;
            }
            downstream.onError(e);
        }

        @Override
        public void onSubscribe(final Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void dispose() {
            disposed = true;
            scheduler.scheduleDirect(this);
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void run() {
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }
    }

}
