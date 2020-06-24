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
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.fuseable.ConditionalSubscriber;
import yourpck.reactivex.internal.subscribers.BasicFuseableConditionalSubscriber;
import yourpck.reactivex.internal.subscribers.BasicFuseableSubscriber;

public final class FlowableMap<T, U> extends AbstractFlowableWithUpstream<T, U> {
    final Function<? super T, ? extends U> mapper;

    public FlowableMap(Flowable<T> source, Function<? super T, ? extends U> mapper) {
        super(source);
        this.mapper = mapper;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new MapConditionalSubscriber<T, U>((ConditionalSubscriber<? super U>) s, mapper));
        } else {
            source.subscribe(new MapSubscriber<T, U>(s, mapper));
        }
    }

    static final class MapSubscriber<T, U> extends BasicFuseableSubscriber<T, U> {
        final Function<? super T, ? extends U> mapper;

        MapSubscriber(Subscriber<? super U> actual, Function<? super T, ? extends U> mapper) {
            super(actual);
            this.mapper = mapper;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return;
            }

            U v;

            try {
                v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            downstream.onNext(v);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public U poll() throws Exception {
            T t = qs.poll();
            return t != null ? ObjectHelper.<U>requireNonNull(mapper.apply(t), "The mapper function returned a null value.") : null;
        }
    }

    static final class MapConditionalSubscriber<T, U> extends BasicFuseableConditionalSubscriber<T, U> {
        final Function<? super T, ? extends U> mapper;

        MapConditionalSubscriber(ConditionalSubscriber<? super U> actual, Function<? super T, ? extends U> function) {
            super(actual);
            this.mapper = function;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            if (sourceMode != NONE) {
                downstream.onNext(null);
                return;
            }

            U v;

            try {
                v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return;
            }
            downstream.onNext(v);
        }

        @Override
        public boolean tryOnNext(T t) {
            if (done) {
                return false;
            }

            U v;

            try {
                v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
            } catch (Throwable ex) {
                fail(ex);
                return true;
            }
            return downstream.tryOnNext(v);
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public U poll() throws Exception {
            T t = qs.poll();
            return t != null ? ObjectHelper.<U>requireNonNull(mapper.apply(t), "The mapper function returned a null value.") : null;
        }
    }

}
