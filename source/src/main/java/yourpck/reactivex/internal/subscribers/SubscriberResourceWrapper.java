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

package yourpck.reactivex.internal.subscribers;

import java.util.concurrent.atomic.AtomicReference;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;

public final class SubscriberResourceWrapper<T> extends AtomicReference<Disposable> implements FlowableSubscriber<T>, Disposable, Subscription {

    private static final long serialVersionUID = -8612022020200669122L;

    final Subscriber<? super T> downstream;

    final AtomicReference<Subscription> upstream = new AtomicReference<Subscription>();

    public SubscriberResourceWrapper(Subscriber<? super T> downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(upstream, s)) {
            downstream.onSubscribe(this);
        }
    }

    @Override
    public void onNext(T t) {
        downstream.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
        DisposableHelper.dispose(this);
        downstream.onError(t);
    }

    @Override
    public void onComplete() {
        DisposableHelper.dispose(this);
        downstream.onComplete();
    }

    @Override
    public void request(long n) {
        if (SubscriptionHelper.validate(n)) {
            upstream.get().request(n);
        }
    }

    @Override
    public void dispose() {
        SubscriptionHelper.cancel(upstream);

        DisposableHelper.dispose(this);
    }

    @Override
    public boolean isDisposed() {
        return upstream.get() == SubscriptionHelper.CANCELLED;
    }

    @Override
    public void cancel() {
        dispose();
    }

    public void setResource(Disposable resource) {
        DisposableHelper.set(this, resource);
    }
}
