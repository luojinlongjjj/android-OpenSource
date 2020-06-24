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
import yourpck.reactivex.internal.subscriptions.DeferredScalarSubscription;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;

public final class FlowableTakeLastOne<T> extends AbstractFlowableWithUpstream<T, T> {

    public FlowableTakeLastOne(Flowable<T> source) {
        super(source);
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new TakeLastOneSubscriber<T>(s));
    }

    static final class TakeLastOneSubscriber<T> extends DeferredScalarSubscription<T>
            implements FlowableSubscriber<T> {

        private static final long serialVersionUID = -5467847744262967226L;

        Subscription upstream;

        TakeLastOneSubscriber(Subscriber<? super T> downstream) {
            super(downstream);
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
            value = t;
        }

        @Override
        public void onError(Throwable t) {
            value = null;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            T v = value;
            if (v != null) {
                complete(v);
            } else {
                downstream.onComplete();
            }
        }

        @Override
        public void cancel() {
            super.cancel();
            upstream.cancel();
        }
    }
}
