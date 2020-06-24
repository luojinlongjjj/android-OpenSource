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

import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.org.reactivestreams.Subscription;
import yourpck.reactivex.FlowableSubscriber;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;
import yourpck.reactivex.internal.util.NotificationLite;

public final class BlockingSubscriber<T> extends AtomicReference<Subscription> implements FlowableSubscriber<T>, Subscription {

    public static final Object TERMINATED = new Object();
    private static final long serialVersionUID = -4875965440900746268L;
    final Queue<Object> queue;

    public BlockingSubscriber(Queue<Object> queue) {
        this.queue = queue;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (SubscriptionHelper.setOnce(this, s)) {
            queue.offer(NotificationLite.subscription(this));
        }
    }

    @Override
    public void onNext(T t) {
        queue.offer(NotificationLite.next(t));
    }

    @Override
    public void onError(Throwable t) {
        queue.offer(NotificationLite.error(t));
    }

    @Override
    public void onComplete() {
        queue.offer(NotificationLite.complete());
    }

    @Override
    public void request(long n) {
        get().request(n);
    }

    @Override
    public void cancel() {
        if (SubscriptionHelper.cancel(this)) {
            queue.offer(TERMINATED);
        }
    }

    public boolean isCancelled() {
        return get() == SubscriptionHelper.CANCELLED;
    }
}
