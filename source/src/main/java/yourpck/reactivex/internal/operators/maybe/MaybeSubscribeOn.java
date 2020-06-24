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

import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.Scheduler;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.disposables.SequentialDisposable;

/**
 * Subscribes to the upstream MaybeSource on the specified scheduler.
 *
 * @param <T> the value type delivered
 */
public final class MaybeSubscribeOn<T> extends AbstractMaybeWithUpstream<T, T> {

    final Scheduler scheduler;

    public MaybeSubscribeOn(MaybeSource<T> source, Scheduler scheduler) {
        super(source);
        this.scheduler = scheduler;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        SubscribeOnMaybeObserver<T> parent = new SubscribeOnMaybeObserver<T>(observer);
        observer.onSubscribe(parent);

        parent.task.replace(scheduler.scheduleDirect(new SubscribeTask<T>(parent, source)));
    }

    static final class SubscribeTask<T> implements Runnable {
        final MaybeObserver<? super T> observer;
        final MaybeSource<T> source;

        SubscribeTask(MaybeObserver<? super T> observer, MaybeSource<T> source) {
            this.observer = observer;
            this.source = source;
        }

        @Override
        public void run() {
            source.subscribe(observer);
        }
    }

    static final class SubscribeOnMaybeObserver<T>
            extends AtomicReference<Disposable>
            implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = 8571289934935992137L;
        final SequentialDisposable task;
        final MaybeObserver<? super T> downstream;

        SubscribeOnMaybeObserver(MaybeObserver<? super T> downstream) {
            this.downstream = downstream;
            this.task = new SequentialDisposable();
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
            task.dispose();
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }

        @Override
        public void onSuccess(T value) {
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
