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

import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.annotations.Nullable;
import yourpck.reactivex.functions.Predicate;
import yourpck.reactivex.internal.fuseable.ConditionalSubscriber;
import yourpck.reactivex.internal.fuseable.QueueSubscription;
import yourpck.reactivex.internal.subscribers.BasicFuseableConditionalSubscriber;
import yourpck.reactivex.internal.subscribers.BasicFuseableSubscriber;

public final class FlowableFilter<T> extends AbstractFlowableWithUpstream<T, T> {
    final Predicate<? super T> predicate;

    public FlowableFilter(Flowable<T> source, Predicate<? super T> predicate) {
        super(source);
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new FilterConditionalSubscriber<T>(
                    (ConditionalSubscriber<? super T>) s, predicate));
        } else {
            source.subscribe(new FilterSubscriber<T>(s, predicate));
        }
    }

    static final class FilterSubscriber<T> extends BasicFuseableSubscriber<T, T>
            implements ConditionalSubscriber<T> {
        final Predicate<? super T> filter;

        FilterSubscriber(Subscriber<? super T> actual, Predicate<? super T> filter) {
            super(actual);
            this.filter = filter;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                upstream.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }
            if (sourceMode != NONE) {
                downstream.onNext(null);
                return true;
            }
            boolean b;
            try {
                b = filter.test(t);
            } catch (Throwable e) {
                fail(e);
                return true;
            }
            if (b) {
                downstream.onNext(t);
            }
            return b;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            QueueSubscription<T> qs = this.qs;
            Predicate<? super T> f = filter;

            for (; ; ) {
                T t = qs.poll();
                if (t == null) {
                    return null;
                }

                if (f.test(t)) {
                    return t;
                }

                if (sourceMode == ASYNC) {
                    qs.request(1);
                }
            }
        }
    }

    static final class FilterConditionalSubscriber<T> extends BasicFuseableConditionalSubscriber<T, T> {
        final Predicate<? super T> filter;

        FilterConditionalSubscriber(ConditionalSubscriber<? super T> actual, Predicate<? super T> filter) {
            super(actual);
            this.filter = filter;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                upstream.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }

            if (sourceMode != NONE) {
                return downstream.tryOnNext(null);
            }

            boolean b;
            try {
                b = filter.test(t);
            } catch (Throwable e) {
                fail(e);
                return true;
            }
            return b && downstream.tryOnNext(t);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            QueueSubscription<T> qs = this.qs;
            Predicate<? super T> f = filter;

            for (; ; ) {
                T t = qs.poll();
                if (t == null) {
                    return null;
                }

                if (f.test(t)) {
                    return t;
                }

                if (sourceMode == ASYNC) {
                    qs.request(1);
                }
            }
        }
    }
}
