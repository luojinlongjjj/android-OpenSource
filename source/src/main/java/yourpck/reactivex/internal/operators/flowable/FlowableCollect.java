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

import java.util.concurrent.Callable;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.BiConsumer;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.subscriptions.DeferredScalarSubscription;
import yourpck.reactivex.internal.subscriptions.EmptySubscription;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class FlowableCollect<T, U> extends AbstractFlowableWithUpstream<T, U> {

    final Callable<? extends U> initialSupplier;
    final BiConsumer<? super U, ? super T> collector;

    public FlowableCollect(Flowable<T> source, Callable<? extends U> initialSupplier, BiConsumer<? super U, ? super T> collector) {
        super(source);
        this.initialSupplier = initialSupplier;
        this.collector = collector;
    }

    @Override
    protected void subscribeActual(Subscriber<? super U> s) {
        U u;
        try {
            u = ObjectHelper.requireNonNull(initialSupplier.call(), "The initial value supplied is null");
        } catch (Throwable e) {
            EmptySubscription.error(e, s);
            return;
        }

        source.subscribe(new CollectSubscriber<T, U>(s, u, collector));
    }

    static final class CollectSubscriber<T, U> extends DeferredScalarSubscription<U> implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -3589550218733891694L;

        final BiConsumer<? super U, ? super T> collector;

        final U u;

        Subscription upstream;

        boolean done;

        CollectSubscriber(Subscriber<? super U> actual, U u, BiConsumer<? super U, ? super T> collector) {
            super(actual);
            this.collector = collector;
            this.u = u;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
                s.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            try {
                collector.accept(u, t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.cancel();
                onError(e);
            }
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
            complete(u);
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
        }
    }
}
