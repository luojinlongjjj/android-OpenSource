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

package yourpck.reactivex.internal.operators.mixed;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.org.reactivestreams.Publisher;
import yourpck.org.reactivestreams.Subscriber;
import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.CompletableObserver;
import yourpck.reactivex.CompletableSource;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * After Completable completes, it relays the signals
 * of the Publisher to the downstream subscriber.
 *
 * @param <R> the result type of the Publisher and this operator
 * @since 2.1.15
 */
public final class CompletableAndThenPublisher<R> extends Flowable<R> {

    final CompletableSource source;

    final Publisher<? extends R> other;

    public CompletableAndThenPublisher(CompletableSource source,
                                       Publisher<? extends R> other) {
        this.source = source;
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        source.subscribe(new AndThenPublisherSubscriber<R>(s, other));
    }

    static final class AndThenPublisherSubscriber<R>
            extends AtomicReference<Subscription>
            implements FlowableSubscriber<R>, CompletableObserver, Subscription {

        private static final long serialVersionUID = -8948264376121066672L;

        final Subscriber<? super R> downstream;
        final AtomicLong requested;
        Publisher<? extends R> other;
        Disposable upstream;

        AndThenPublisherSubscriber(Subscriber<? super R> downstream, Publisher<? extends R> other) {
            this.downstream = downstream;
            this.other = other;
            this.requested = new AtomicLong();
        }

        @Override
        public void onNext(R t) {
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            Publisher<? extends R> p = other;
            if (p == null) {
                downstream.onComplete();
            } else {
                other = null;
                p.subscribe(this);
            }
        }

        @Override
        public void request(long n) {
            SubscriptionHelper.deferredRequest(this, requested, n);
        }

        @Override
        public void cancel() {
            upstream.dispose();
            SubscriptionHelper.cancel(this);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSubscribe(Subscription s) {
            SubscriptionHelper.deferredSetOnce(this, requested, s);
        }
    }
}
