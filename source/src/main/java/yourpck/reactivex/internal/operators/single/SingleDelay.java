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

import java.util.concurrent.TimeUnit;

import yourpck.reactivex.Scheduler;
import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.SequentialDisposable;

public final class SingleDelay<T> extends Single<T> {

    final SingleSource<? extends T> source;
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;
    final boolean delayError;

    public SingleDelay(SingleSource<? extends T> source, long time, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        this.source = source;
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
        this.delayError = delayError;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> observer) {

        final SequentialDisposable sd = new SequentialDisposable();
        observer.onSubscribe(sd);
        source.subscribe(new Delay(sd, observer));
    }

    final class Delay implements SingleObserver<T> {
        final SingleObserver<? super T> downstream;
        private final SequentialDisposable sd;

        Delay(SequentialDisposable sd, SingleObserver<? super T> observer) {
            this.sd = sd;
            this.downstream = observer;
        }

        @Override
        public void onSubscribe(Disposable d) {
            sd.replace(d);
        }

        @Override
        public void onSuccess(final T value) {
            sd.replace(scheduler.scheduleDirect(new OnSuccess(value), time, unit));
        }

        @Override
        public void onError(final Throwable e) {
            sd.replace(scheduler.scheduleDirect(new OnError(e), delayError ? time : 0, unit));
        }

        final class OnSuccess implements Runnable {
            private final T value;

            OnSuccess(T value) {
                this.value = value;
            }

            @Override
            public void run() {
                downstream.onSuccess(value);
            }
        }

        final class OnError implements Runnable {
            private final Throwable e;

            OnError(Throwable e) {
                this.e = e;
            }

            @Override
            public void run() {
                downstream.onError(e);
            }
        }
    }
}
