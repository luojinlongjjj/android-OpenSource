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

package yourpck.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.org.reactivestreams.Publisher;
import yourpck.org.reactivestreams.Subscriber;
import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.exceptions.MissingBackpressureException;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.fuseable.QueueSubscription;
import yourpck.reactivex.internal.fuseable.SimpleQueue;
import yourpck.reactivex.internal.queue.SpscArrayQueue;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.internal.util.AtomicThrowable;
import yourpck.reactivex.internal.util.BackpressureHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class FlowableSwitchMap<T, R> extends AbstractFlowableWithUpstream<T, R> {
    final Function<? super T, ? extends Publisher<? extends R>> mapper;
    final int bufferSize;
    final boolean delayErrors;

    public FlowableSwitchMap(Flowable<T> source,
                             Function<? super T, ? extends Publisher<? extends R>> mapper, int bufferSize,
                             boolean delayErrors) {
        super(source);
        this.mapper = mapper;
        this.bufferSize = bufferSize;
        this.delayErrors = delayErrors;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        if (FlowableScalarXMap.tryScalarXMapSubscribe(source, s, mapper)) {
            return;
        }
        source.subscribe(new SwitchMapSubscriber<T, R>(s, mapper, bufferSize, delayErrors));
    }

    static final class SwitchMapSubscriber<T, R> extends AtomicInteger implements FlowableSubscriber<T>, Subscription {

        static final SwitchMapInnerSubscriber<Object, Object> CANCELLED;
        private static final long serialVersionUID = -3491074160481096299L;

        static {
            CANCELLED = new SwitchMapInnerSubscriber<Object, Object>(null, -1L, 1);
            CANCELLED.cancel();
        }

        final Subscriber<? super R> downstream;
        final Function<? super T, ? extends Publisher<? extends R>> mapper;
        final int bufferSize;
        final boolean delayErrors;
        final AtomicThrowable error;
        final AtomicReference<SwitchMapInnerSubscriber<T, R>> active = new AtomicReference<SwitchMapInnerSubscriber<T, R>>();
        final AtomicLong requested = new AtomicLong();
        volatile boolean done;
        volatile boolean cancelled;
        Subscription upstream;
        volatile long unique;

        SwitchMapSubscriber(Subscriber<? super R> actual,
                            Function<? super T, ? extends Publisher<? extends R>> mapper, int bufferSize,
                            boolean delayErrors) {
            this.downstream = actual;
            this.mapper = mapper;
            this.bufferSize = bufferSize;
            this.delayErrors = delayErrors;
            this.error = new AtomicThrowable();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            long c = unique + 1;
            unique = c;

            SwitchMapInnerSubscriber<T, R> inner = active.get();
            if (inner != null) {
                inner.cancel();
            }

            Publisher<? extends R> p;
            try {
                p = ObjectHelper.requireNonNull(mapper.apply(t), "The publisher returned is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.cancel();
                onError(e);
                return;
            }

            SwitchMapInnerSubscriber<T, R> nextInner = new SwitchMapInnerSubscriber<T, R>(this, c, bufferSize);

            for (; ; ) {
                inner = active.get();
                if (inner == CANCELLED) {
                    break;
                }
                if (active.compareAndSet(inner, nextInner)) {
                    p.subscribe(nextInner);
                    break;
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            if (!done && error.addThrowable(t)) {
                if (!delayErrors) {
                    disposeInner();
                }
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            drain();
        }

        @Override
        public void request(long n) {
            if (SubscriptionHelper.validate(n)) {
                BackpressureHelper.add(requested, n);
                if (unique == 0L) {
                    upstream.request(Long.MAX_VALUE);
                } else {
                    drain();
                }
            }
        }

        @Override
        public void cancel() {
            if (!cancelled) {
                cancelled = true;
                upstream.cancel();

                disposeInner();
            }
        }

        @SuppressWarnings("unchecked")
        void disposeInner() {
            SwitchMapInnerSubscriber<T, R> a = active.get();
            if (a != CANCELLED) {
                a = active.getAndSet((SwitchMapInnerSubscriber<T, R>) CANCELLED);
                if (a != CANCELLED && a != null) {
                    a.cancel();
                }
            }
        }

        void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            final Subscriber<? super R> a = downstream;

            int missing = 1;

            for (; ; ) {

                if (cancelled) {
                    active.lazySet(null);
                    return;
                }

                if (done) {
                    if (delayErrors) {
                        if (active.get() == null) {
                            Throwable err = error.get();
                            if (err != null) {
                                a.onError(error.terminate());
                            } else {
                                a.onComplete();
                            }
                            return;
                        }
                    } else {
                        Throwable err = error.get();
                        if (err != null) {
                            disposeInner();
                            a.onError(error.terminate());
                            return;
                        } else if (active.get() == null) {
                            a.onComplete();
                            return;
                        }
                    }
                }

                SwitchMapInnerSubscriber<T, R> inner = active.get();
                SimpleQueue<R> q = inner != null ? inner.queue : null;
                if (q != null) {
                    if (inner.done) {
                        if (!delayErrors) {
                            Throwable err = error.get();
                            if (err != null) {
                                disposeInner();
                                a.onError(error.terminate());
                                return;
                            } else if (q.isEmpty()) {
                                active.compareAndSet(inner, null);
                                continue;
                            }
                        } else {
                            if (q.isEmpty()) {
                                active.compareAndSet(inner, null);
                                continue;
                            }
                        }
                    }

                    long r = requested.get();
                    long e = 0L;
                    boolean retry = false;

                    while (e != r) {
                        if (cancelled) {
                            return;
                        }

                        boolean d = inner.done;
                        R v;

                        try {
                            v = q.poll();
                        } catch (Throwable ex) {
                            Exceptions.throwIfFatal(ex);
                            inner.cancel();
                            error.addThrowable(ex);
                            d = true;
                            v = null;
                        }
                        boolean empty = v == null;

                        if (inner != active.get()) {
                            retry = true;
                            break;
                        }

                        if (d) {
                            if (!delayErrors) {
                                Throwable err = error.get();
                                if (err != null) {
                                    a.onError(error.terminate());
                                    return;
                                } else if (empty) {
                                    active.compareAndSet(inner, null);
                                    retry = true;
                                    break;
                                }
                            } else {
                                if (empty) {
                                    active.compareAndSet(inner, null);
                                    retry = true;
                                    break;
                                }
                            }
                        }

                        if (empty) {
                            break;
                        }

                        a.onNext(v);

                        e++;
                    }

                    if (e != 0L) {
                        if (!cancelled) {
                            if (r != Long.MAX_VALUE) {
                                requested.addAndGet(-e);
                            }
                            inner.get().request(e);
                        }
                    }

                    if (retry) {
                        continue;
                    }
                }

                missing = addAndGet(-missing);
                if (missing == 0) {
                    break;
                }
            }
        }
    }

    static final class SwitchMapInnerSubscriber<T, R>
            extends AtomicReference<Subscription> implements FlowableSubscriber<R> {

        private static final long serialVersionUID = 3837284832786408377L;
        final SwitchMapSubscriber<T, R> parent;
        final long index;
        final int bufferSize;

        volatile SimpleQueue<R> queue;

        volatile boolean done;

        int fusionMode;

        SwitchMapInnerSubscriber(SwitchMapSubscriber<T, R> parent, long index, int bufferSize) {
            this.parent = parent;
            this.index = index;
            this.bufferSize = bufferSize;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this, s)) {
                if (s instanceof QueueSubscription) {
                    @SuppressWarnings("unchecked")
                    QueueSubscription<R> qs = (QueueSubscription<R>) s;

                    int m = qs.requestFusion(QueueSubscription.ANY | QueueSubscription.BOUNDARY);
                    if (m == QueueSubscription.SYNC) {
                        fusionMode = m;
                        queue = qs;
                        done = true;
                        parent.drain();
                        return;
                    }
                    if (m == QueueSubscription.ASYNC) {
                        fusionMode = m;
                        queue = qs;
                        s.request(bufferSize);
                        return;
                    }
                }

                queue = new SpscArrayQueue<R>(bufferSize);

                s.request(bufferSize);
            }
        }

        @Override
        public void onNext(R t) {
            SwitchMapSubscriber<T, R> p = parent;
            if (index == p.unique) {
                if (fusionMode == QueueSubscription.NONE && !queue.offer(t)) {
                    onError(new MissingBackpressureException("Queue full?!"));
                    return;
                }
                p.drain();
            }
        }

        @Override
        public void onError(Throwable t) {
            SwitchMapSubscriber<T, R> p = parent;
            if (index == p.unique && p.error.addThrowable(t)) {
                if (!p.delayErrors) {
                    p.upstream.cancel();
                }
                done = true;
                p.drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            SwitchMapSubscriber<T, R> p = parent;
            if (index == p.unique) {
                done = true;
                p.drain();
            }
        }

        public void cancel() {
            SubscriptionHelper.cancel(this);
        }
    }
}
