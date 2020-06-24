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

import java.util.Collection;
import java.util.concurrent.Callable;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.annotations.Nullable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.fuseable.QueueFuseable;
import yourpck.reactivex.internal.subscribers.BasicFuseableSubscriber;
import yourpck.reactivex.internal.subscriptions.EmptySubscription;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class FlowableDistinct<T, K> extends AbstractFlowableWithUpstream<T, T> {

    final Function<? super T, K> keySelector;

    final Callable<? extends Collection<? super K>> collectionSupplier;

    public FlowableDistinct(Flowable<T> source, Function<? super T, K> keySelector, Callable<? extends Collection<? super K>> collectionSupplier) {
        super(source);
        this.keySelector = keySelector;
        this.collectionSupplier = collectionSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> subscriber) {
        Collection<? super K> collection;

        try {
            collection = ObjectHelper.requireNonNull(collectionSupplier.call(), "The collectionSupplier returned a null collection. Null values are generally not allowed in 2.x operators and sources.");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            EmptySubscription.error(ex, subscriber);
            return;
        }

        source.subscribe(new DistinctSubscriber<T, K>(subscriber, keySelector, collection));
    }

    static final class DistinctSubscriber<T, K> extends BasicFuseableSubscriber<T, T> {

        final Collection<? super K> collection;

        final Function<? super T, K> keySelector;

        DistinctSubscriber(Subscriber<? super T> actual, Function<? super T, K> keySelector, Collection<? super K> collection) {
            super(actual);
            this.keySelector = keySelector;
            this.collection = collection;
        }

        @Override
        public void onNext(T value) {
            if (done) {
                return;
            }
            if (sourceMode == NONE) {
                K key;
                boolean b;

                try {
                    key = ObjectHelper.requireNonNull(keySelector.apply(value), "The keySelector returned a null key");
                    b = collection.add(key);
                } catch (Throwable ex) {
                    fail(ex);
                    return;
                }

                if (b) {
                    downstream.onNext(value);
                } else {
                    upstream.request(1);
                }
            } else {
                downstream.onNext(null);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
            } else {
                done = true;
                collection.clear();
                downstream.onError(e);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                collection.clear();
                downstream.onComplete();
            }
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }

        @Nullable
        @Override
        public T poll() throws Exception {
            for (; ; ) {
                T v = qs.poll();

                if (v == null || collection.add(ObjectHelper.requireNonNull(keySelector.apply(v), "The keySelector returned a null key"))) {
                    return v;
                } else {
                    if (sourceMode == QueueFuseable.ASYNC) {
                        upstream.request(1);
                    }
                }
            }
        }

        @Override
        public void clear() {
            collection.clear();
            super.clear();
        }
    }
}
