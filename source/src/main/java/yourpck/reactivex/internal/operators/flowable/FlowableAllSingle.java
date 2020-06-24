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

import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Predicate;
import yourpck.reactivex.internal.fuseable.FuseToFlowable;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class FlowableAllSingle<T> extends Single<Boolean> implements FuseToFlowable<Boolean> {

    final Flowable<T> source;

    final Predicate<? super T> predicate;

    public FlowableAllSingle(Flowable<T> source, Predicate<? super T> predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    protected void subscribeActual(SingleObserver<? super Boolean> observer) {
        source.subscribe(new AllSubscriber<T>(observer, predicate));
    }

    @Override
    public Flowable<Boolean> fuseToFlowable() {
        return RxJavaPlugins.onAssembly(new FlowableAll<T>(source, predicate));
    }

    static final class AllSubscriber<T> implements FlowableSubscriber<T>, Disposable {

        final SingleObserver<? super Boolean> downstream;

        final Predicate<? super T> predicate;

        Subscription upstream;

        boolean done;

        AllSubscriber(SingleObserver<? super Boolean> actual, Predicate<? super T> predicate) {
            this.downstream = actual;
            this.predicate = predicate;
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
            boolean b;
            try {
                b = predicate.test(t);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                upstream.cancel();
                upstream = SubscriptionHelper.CANCELLED;
                onError(e);
                return;
            }
            if (!b) {
                done = true;
                upstream.cancel();
                upstream = SubscriptionHelper.CANCELLED;
                downstream.onSuccess(false);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            upstream = SubscriptionHelper.CANCELLED;
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            upstream = SubscriptionHelper.CANCELLED;

            downstream.onSuccess(true);
        }

        @Override
        public void dispose() {
            upstream.cancel();
            upstream = SubscriptionHelper.CANCELLED;
        }

        @Override
        public boolean isDisposed() {
            return upstream == SubscriptionHelper.CANCELLED;
        }
    }
}
