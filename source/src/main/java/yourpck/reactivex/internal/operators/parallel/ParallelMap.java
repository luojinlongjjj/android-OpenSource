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

package yourpck.reactivex.internal.operators.parallel;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.fuseable.ConditionalSubscriber;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.parallel.ParallelFlowable;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Maps each 'rail' of the source ParallelFlowable with a mapper function.
 *
 * @param <T> the input value type
 * @param <R> the output value type
 */
public final class ParallelMap<T, R> extends ParallelFlowable<R> {

    final ParallelFlowable<T> source;

    final Function<? super T, ? extends R> mapper;

    public ParallelMap(ParallelFlowable<T> source, Function<? super T, ? extends R> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public void subscribe(Subscriber<? super R>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;
        @SuppressWarnings("unchecked")
        Subscriber<? super T>[] parents = new Subscriber[n];

        for (int i = 0; i < n; i++) {
            Subscriber<? super R> a = subscribers[i];
            if (a instanceof ConditionalSubscriber) {
                parents[i] = new ParallelMapConditionalSubscriber<T, R>((ConditionalSubscriber<? super R>) a, mapper);
            } else {
                parents[i] = new ParallelMapSubscriber<T, R>(a, mapper);
            }
        }

        source.subscribe(parents);
    }

    @Override
    public int parallelism() {
        return source.parallelism();
    }

    static final class ParallelMapSubscriber<T, R> implements FlowableSubscriber<T>, Subscription {

        final Subscriber<? super R> downstream;

        final Function<? super T, ? extends R> mapper;

        Subscription upstream;

        boolean done;

        ParallelMapSubscriber(Subscriber<? super R> actual, Function<? super T, ? extends R> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
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
            R v;

            try {
                v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                cancel();
                onError(ex);
                return;
            }

            downstream.onNext(v);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            downstream.onComplete();
        }

    }

    static final class ParallelMapConditionalSubscriber<T, R> implements ConditionalSubscriber<T>, Subscription {

        final ConditionalSubscriber<? super R> downstream;

        final Function<? super T, ? extends R> mapper;

        Subscription upstream;

        boolean done;

        ParallelMapConditionalSubscriber(ConditionalSubscriber<? super R> actual, Function<? super T, ? extends R> mapper) {
            this.downstream = actual;
            this.mapper = mapper;
        }

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
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
            R v;

            try {
                v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                cancel();
                onError(ex);
                return;
            }

            downstream.onNext(v);
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            R v;

            try {
                v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                cancel();
                onError(ex);
                return false;
            }

            return downstream.tryOnNext(v);
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            downstream.onComplete();
        }

    }
}
