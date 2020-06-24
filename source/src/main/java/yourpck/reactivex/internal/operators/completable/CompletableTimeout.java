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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import yourpck.reactivex.Completable;
import yourpck.reactivex.CompletableObserver;
import yourpck.reactivex.CompletableSource;
import yourpck.reactivex.Scheduler;
import yourpck.reactivex.disposables.CompositeDisposable;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.plugins.RxJavaPlugins;

import static yourpck.reactivex.internal.util.ExceptionHelper.timeoutMessage;

public final class CompletableTimeout extends Completable {

    final CompletableSource source;
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;
    final CompletableSource other;

    public CompletableTimeout(CompletableSource source, long timeout,
                              TimeUnit unit, Scheduler scheduler, CompletableSource other) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    public void subscribeActual(final CompletableObserver observer) {
        final CompositeDisposable set = new CompositeDisposable();
        observer.onSubscribe(set);

        final AtomicBoolean once = new AtomicBoolean();

        Disposable timer = scheduler.scheduleDirect(new DisposeTask(once, set, observer), timeout, unit);

        set.add(timer);

        source.subscribe(new TimeOutObserver(set, once, observer));
    }

    static final class TimeOutObserver implements CompletableObserver {

        private final CompositeDisposable set;
        private final AtomicBoolean once;
        private final CompletableObserver downstream;

        TimeOutObserver(CompositeDisposable set, AtomicBoolean once, CompletableObserver observer) {
            this.set = set;
            this.once = once;
            this.downstream = observer;
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onError(Throwable e) {
            if (once.compareAndSet(false, true)) {
                set.dispose();
                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (once.compareAndSet(false, true)) {
                set.dispose();
                downstream.onComplete();
            }
        }

    }

    final class DisposeTask implements Runnable {
        final CompositeDisposable set;
        final CompletableObserver downstream;
        private final AtomicBoolean once;

        DisposeTask(AtomicBoolean once, CompositeDisposable set, CompletableObserver observer) {
            this.once = once;
            this.set = set;
            this.downstream = observer;
        }

        @Override
        public void run() {
            if (once.compareAndSet(false, true)) {
                set.clear();
                if (other == null) {
                    downstream.onError(new TimeoutException(timeoutMessage(timeout, unit)));
                } else {
                    other.subscribe(new DisposeObserver());
                }
            }
        }

        final class DisposeObserver implements CompletableObserver {

            @Override
            public void onSubscribe(Disposable d) {
                set.add(d);
            }

            @Override
            public void onError(Throwable e) {
                set.dispose();
                downstream.onError(e);
            }

            @Override
            public void onComplete() {
                set.dispose();
                downstream.onComplete();
            }

        }
    }
}
