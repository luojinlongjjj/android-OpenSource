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
import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Action;
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.functions.LongConsumer;
import yourpck.reactivex.internal.subscriptions.EmptySubscription;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class FlowableDoOnLifecycle<T> extends AbstractFlowableWithUpstream<T, T> {
    private final Consumer<? super Subscription> onSubscribe;
    private final LongConsumer onRequest;
    private final Action onCancel;

    public FlowableDoOnLifecycle(Flowable<T> source, Consumer<? super Subscription> onSubscribe,
                                 LongConsumer onRequest, Action onCancel) {
        super(source);
        this.onSubscribe = onSubscribe;
        this.onRequest = onRequest;
        this.onCancel = onCancel;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new SubscriptionLambdaSubscriber<T>(s, onSubscribe, onRequest, onCancel));
    }

    static final class SubscriptionLambdaSubscriber<T> implements FlowableSubscriber<T>, Subscription {
        final Subscriber<? super T> downstream;
        final Consumer<? super Subscription> onSubscribe;
        final LongConsumer onRequest;
        final Action onCancel;

        Subscription upstream;

        SubscriptionLambdaSubscriber(Subscriber<? super T> actual,
                                     Consumer<? super Subscription> onSubscribe,
                                     LongConsumer onRequest,
                                     Action onCancel) {
            this.downstream = actual;
            this.onSubscribe = onSubscribe;
            this.onCancel = onCancel;
            this.onRequest = onRequest;
        }

        @Override
        public void onSubscribe(Subscription s) {
            // this way, multiple calls to onSubscribe can show up in tests that use doOnSubscribe to validate behavior
            try {
                onSubscribe.accept(s);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.cancel();
                this.upstream = SubscriptionHelper.CANCELLED;
                EmptySubscription.error(e, downstream);
                return;
            }
            if (SubscriptionHelper.validate(this.upstream, s)) {
                this.upstream = s;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (upstream != SubscriptionHelper.CANCELLED) {
                downstream.onError(t);
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (upstream != SubscriptionHelper.CANCELLED) {
                downstream.onComplete();
            }
        }

        @Override
        public void request(long n) {
            try {
                onRequest.accept(n);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                RxJavaPlugins.onError(e);
            }
            upstream.request(n);
        }

        @Override
        public void cancel() {
            Subscription s = upstream;
            if (s != SubscriptionHelper.CANCELLED) {
                upstream = SubscriptionHelper.CANCELLED;
                try {
                    onCancel.run();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    RxJavaPlugins.onError(e);
                }
                s.cancel();
            }
        }
    }
}
