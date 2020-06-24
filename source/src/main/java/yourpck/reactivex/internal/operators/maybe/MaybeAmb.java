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

import java.util.concurrent.atomic.AtomicBoolean;

import yourpck.reactivex.Maybe;
import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.disposables.CompositeDisposable;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Signals the event of the first MaybeSource that signals.
 *
 * @param <T> the value type emitted
 */
public final class MaybeAmb<T> extends Maybe<T> {
    private final MaybeSource<? extends T>[] sources;
    private final Iterable<? extends MaybeSource<? extends T>> sourcesIterable;

    public MaybeAmb(MaybeSource<? extends T>[] sources, Iterable<? extends MaybeSource<? extends T>> sourcesIterable) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        MaybeSource<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new MaybeSource[8];
            try {
                for (MaybeSource<? extends T> element : sourcesIterable) {
                    if (element == null) {
                        EmptyDisposable.error(new NullPointerException("One of the sources is null"), observer);
                        return;
                    }
                    if (count == sources.length) {
                        MaybeSource<? extends T>[] b = new MaybeSource[count + (count >> 2)];
                        System.arraycopy(sources, 0, b, 0, count);
                        sources = b;
                    }
                    sources[count++] = element;
                }
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                EmptyDisposable.error(e, observer);
                return;
            }
        } else {
            count = sources.length;
        }

        CompositeDisposable set = new CompositeDisposable();
        observer.onSubscribe(set);

        AtomicBoolean winner = new AtomicBoolean();

        for (int i = 0; i < count; i++) {
            MaybeSource<? extends T> s = sources[i];
            if (set.isDisposed()) {
                return;
            }

            if (s == null) {
                set.dispose();
                NullPointerException ex = new NullPointerException("One of the MaybeSources is null");
                if (winner.compareAndSet(false, true)) {
                    observer.onError(ex);
                } else {
                    RxJavaPlugins.onError(ex);
                }
                return;
            }

            s.subscribe(new AmbMaybeObserver<T>(observer, set, winner));
        }

        if (count == 0) {
            observer.onComplete();
        }
    }

    static final class AmbMaybeObserver<T>
            implements MaybeObserver<T> {

        final MaybeObserver<? super T> downstream;

        final AtomicBoolean winner;

        final CompositeDisposable set;

        Disposable upstream;

        AmbMaybeObserver(MaybeObserver<? super T> downstream, CompositeDisposable set, AtomicBoolean winner) {
            this.downstream = downstream;
            this.set = set;
            this.winner = winner;
        }

        @Override
        public void onSubscribe(Disposable d) {
            upstream = d;
            set.add(d);
        }

        @Override
        public void onSuccess(T value) {
            if (winner.compareAndSet(false, true)) {
                set.delete(upstream);
                set.dispose();

                downstream.onSuccess(value);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (winner.compareAndSet(false, true)) {
                set.delete(upstream);
                set.dispose();

                downstream.onError(e);
            } else {
                RxJavaPlugins.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (winner.compareAndSet(false, true)) {
                set.delete(upstream);
                set.dispose();

                downstream.onComplete();
            }
        }
    }
}
