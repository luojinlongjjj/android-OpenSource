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

import java.util.concurrent.atomic.AtomicInteger;

import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.CompositeDisposable;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class SingleEquals<T> extends Single<Boolean> {

    final SingleSource<? extends T> first;
    final SingleSource<? extends T> second;

    public SingleEquals(SingleSource<? extends T> first, SingleSource<? extends T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super Boolean> observer) {

        final AtomicInteger count = new AtomicInteger();
        final Object[] values = {null, null};

        final CompositeDisposable set = new CompositeDisposable();
        observer.onSubscribe(set);

        first.subscribe(new InnerObserver<T>(0, set, values, observer, count));
        second.subscribe(new InnerObserver<T>(1, set, values, observer, count));
    }

    static class InnerObserver<T> implements SingleObserver<T> {
        final int index;
        final CompositeDisposable set;
        final Object[] values;
        final SingleObserver<? super Boolean> downstream;
        final AtomicInteger count;

        InnerObserver(int index, CompositeDisposable set, Object[] values, SingleObserver<? super Boolean> observer, AtomicInteger count) {
            this.index = index;
            this.set = set;
            this.values = values;
            this.downstream = observer;
            this.count = count;
        }

        @Override
        public void onSubscribe(Disposable d) {
            set.add(d);
        }

        @Override
        public void onSuccess(T value) {
            values[index] = value;

            if (count.incrementAndGet() == 2) {
                downstream.onSuccess(ObjectHelper.equals(values[0], values[1]));
            }
        }

        @Override
        public void onError(Throwable e) {
            for (; ; ) {
                int state = count.get();
                if (state >= 2) {
                    RxJavaPlugins.onError(e);
                    return;
                }
                if (count.compareAndSet(state, 2)) {
                    set.dispose();
                    downstream.onError(e);
                    return;
                }
            }
        }

    }

}
