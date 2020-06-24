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
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.internal.fuseable.ConditionalSubscriber;
import yourpck.reactivex.internal.subscribers.BasicFuseableConditionalSubscriber;
import yourpck.reactivex.internal.subscribers.BasicFuseableSubscriber;

/**
 * Calls a consumer after pushing the current item to the downstream.
 * <p>History: 2.0.1 - experimental
 *
 * @param <T> the value type
 * @since 2.1
 */
public final class FlowableDoAfterNext<T> extends AbstractFlowableWithUpstream<T, T> {

    final Consumer<? super T> onAfterNext;

    public FlowableDoAfterNext(Flowable<T> source, Consumer<? super T> onAfterNext) {
        super(source);
        this.onAfterNext = onAfterNext;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new DoAfterConditionalSubscriber<T>((ConditionalSubscriber<? super T>) s, onAfterNext));
        } else {
            source.subscribe(new DoAfterSubscriber<T>(s, onAfterNext));
        }
    }

    static final class DoAfterSubscriber<T> extends BasicFuseableSubscriber<T, T> {

        final Consumer<? super T> onAfterNext;

        DoAfterSubscriber(Subscriber<? super T> actual, Consumer<? super T> onAfterNext) {
            super(actual);
            this.onAfterNext = onAfterNext;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            downstream.onNext(t);

            if (sourceMode == NONE) {
                try {
                    onAfterNext.accept(t);
                } catch (Throwable ex) {
                    fail(ex);
                }
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            T v = qs.poll();
            if (v != null) {
                onAfterNext.accept(v);
            }
            return v;
        }
    }

    static final class DoAfterConditionalSubscriber<T> extends BasicFuseableConditionalSubscriber<T, T> {

        final Consumer<? super T> onAfterNext;

        DoAfterConditionalSubscriber(ConditionalSubscriber<? super T> actual, Consumer<? super T> onAfterNext) {
            super(actual);
            this.onAfterNext = onAfterNext;
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);

            if (sourceMode == NONE) {
                try {
                    onAfterNext.accept(t);
                } catch (Throwable ex) {
                    fail(ex);
                }
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            boolean b = downstream.tryOnNext(t);
            try {
                onAfterNext.accept(t);
            } catch (Throwable ex) {
                fail(ex);
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
            T v = qs.poll();
            if (v != null) {
                onAfterNext.accept(v);
            }
            return v;
        }
    }
}
