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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import yourpck.reactivex.Completable;
import yourpck.reactivex.CompletableObserver;
import yourpck.reactivex.CompletableSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.internal.disposables.SequentialDisposable;
import yourpck.reactivex.internal.functions.ObjectHelper;

public final class CompletableConcatIterable extends Completable {
    final Iterable<? extends CompletableSource> sources;

    public CompletableConcatIterable(Iterable<? extends CompletableSource> sources) {
        this.sources = sources;
    }

    @Override
    public void subscribeActual(CompletableObserver observer) {

        Iterator<? extends CompletableSource> it;

        try {
            it = ObjectHelper.requireNonNull(sources.iterator(), "The iterator returned is null");
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, observer);
            return;
        }

        ConcatInnerObserver inner = new ConcatInnerObserver(observer, it);
        observer.onSubscribe(inner.sd);
        inner.next();
    }

    static final class ConcatInnerObserver extends AtomicInteger implements CompletableObserver {

        private static final long serialVersionUID = -7965400327305809232L;

        final CompletableObserver downstream;
        final Iterator<? extends CompletableSource> sources;

        final SequentialDisposable sd;

        ConcatInnerObserver(CompletableObserver actual, Iterator<? extends CompletableSource> sources) {
            this.downstream = actual;
            this.sources = sources;
            this.sd = new SequentialDisposable();
        }

        @Override
        public void onSubscribe(Disposable d) {
            sd.replace(d);
        }

        @Override
        public void onError(Throwable e) {
            downstream.onError(e);
        }

        @Override
        public void onComplete() {
            next();
        }

        void next() {
            if (sd.isDisposed()) {
                return;
            }

            if (getAndIncrement() != 0) {
                return;
            }

            Iterator<? extends CompletableSource> a = sources;
            do {
                if (sd.isDisposed()) {
                    return;
                }

                boolean b;
                try {
                    b = a.hasNext();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    downstream.onError(ex);
                    return;
                }

                if (!b) {
                    downstream.onComplete();
                    return;
                }

                CompletableSource c;

                try {
                    c = ObjectHelper.requireNonNull(a.next(), "The CompletableSource returned is null");
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    downstream.onError(ex);
                    return;
                }

                c.subscribe(this);
            } while (decrementAndGet() != 0);
        }
    }
}
