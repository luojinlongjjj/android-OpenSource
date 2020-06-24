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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.annotations.Nullable;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.subscriptions.BasicIntQueueSubscription;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.internal.util.BackpressureHelper;

/**
 * Maps a success value into an Iterable and streams it back as a Flowable.
 *
 * @param <T> the source value type
 * @param <R> the element type of the Iterable
 */
public final class SingleFlatMapIterableFlowable<T, R> extends Flowable<R> {

    final SingleSource<T> source;

    final Function<? super T, ? extends Iterable<? extends R>> mapper;

    public SingleFlatMapIterableFlowable(SingleSource<T> source,
                                         Function<? super T, ? extends Iterable<? extends R>> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new FlatMapIterableObserver<T, R>(s, mapper));
    }

    static final class FlatMapIterableObserver<T, R>
            extends BasicIntQueueSubscription<R>
            implements SingleObserver<T> {

        private static final long serialVersionUID = -8938804753851907758L;

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends Iterable<? extends R>> mapper;

        final AtomicLong requested;

        Disposable upstream;

        volatile Iterator<? extends R> it;

        volatile boolean cancelled;

        boolean outputFused;

        FlatMapIterableObserver(Subscriber<? super R> actual,
                                Function<? super T, ? extends Iterable<? extends R>> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
            this.requested = new AtomicLong();
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            Iterator<? extends R> iterator;
            boolean has;
            try {
                iterator = mapper.apply(value).iterator();

                has = iterator.hasNext();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(ex);
                return;
            }

            if (!has) {
                downstream.onComplete();
                return;
            }

            this.it = iterator;
            drain();
        }

        @Override
        public void onError(Throwable e) {
            upstream = DisposableHelper.DISPOSED;
            downstream.onError(e);
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstream.dispose();
            upstream = DisposableHelper.DISPOSED;
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            Subscriber<? super R> a = downstream;
            Iterator<? extends R> iterator = this.it;

            if (outputFused && iterator != null) {
                a.onNext(null);
                a.onComplete();
                return;
            }

            int missed = 1;

            for (; ; ) {

                if (iterator != null) {
                    long r = requested.get();
                    long e = 0L;

                    if (r == Long.MAX_VALUE) {
                        slowPath(a, iterator);
                        return;
                    }

                    while (e != r) {
                        if (cancelled) {
                            return;
                        }

                        R v;

                        try {
                            v = ObjectHelper.requireNonNull(iterator.next(), "The iterator returned a null value");
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            a.onError(ex);
                            return;
                        }

                        a.onNext(v);

                        if (cancelled) {
                            return;
                        }

                        e++;

                        boolean b;

                        try {
                            b = iterator.hasNext();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            a.onError(ex);
                            return;
                        }

                        if (!b) {
                            a.onComplete();
                            return;
                        }
                    }

                    if (e != 0L) {
                        BackpressureHelper.produced(requested, e);
                    }
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }

                if (iterator == null) {
                    iterator = it;
                }
            }
        }

        void slowPath(Subscriber<? super R> a, Iterator<? extends R> iterator) {
            for (; ; ) {
                if (cancelled) {
                    return;
                }

                R v;

                try {
                    v = iterator.next();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    a.onError(ex);
                    return;
                }

                a.onNext(v);

                if (cancelled) {
                    return;
                }

                boolean b;

                try {
                    b = iterator.hasNext();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    a.onError(ex);
                    return;
                }

                if (!b) {
                    a.onComplete();
                    return;
                }
            }
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & ASYNC) != 0) {
                outputFused = true;
                return ASYNC;
            }
            return NONE;
        }

        @Override
        public void clear() {
            it = null;
        }

        @Override
        public boolean isEmpty() {
            return it == null;
        }

        @Nullable
        @Override
        public R poll() throws Exception {
            Iterator<? extends R> iterator = it;

            if (iterator != null) {
                R v = ObjectHelper.requireNonNull(iterator.next(), "The iterator returned a null value");
                if (!iterator.hasNext()) {
                    it = null;
                }
                return v;
            }
            return null;
        }

    }
}