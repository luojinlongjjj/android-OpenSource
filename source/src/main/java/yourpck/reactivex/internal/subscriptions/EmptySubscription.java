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

package yourpck.reactivex.internal.subscriptions;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.annotations.Nullable;
import yourpck.reactivex.internal.fuseable.QueueSubscription;

/**
 * An empty subscription that does nothing other than validates the request amount.
 */
public enum EmptySubscription implements QueueSubscription<Object> {
    /**
     * A singleton, stateless instance.
     */
    INSTANCE;

    /**
     * Sets the empty subscription instance on the subscriber and then
     * calls onError with the supplied error.
     *
     * <p>Make sure this is only called if the subscriber hasn't received a
     * subscription already (there is no way of telling this).
     *
     * @param e the error to deliver to the subscriber
     * @param s the target subscriber
     */
    public static void error(Throwable e, Subscriber<?> s) {
        s.onSubscribe(INSTANCE);
        s.onError(e);
    }

    /**
     * Sets the empty subscription instance on the subscriber and then
     * calls onComplete.
     *
     * <p>Make sure this is only called if the subscriber hasn't received a
     * subscription already (there is no way of telling this).
     *
     * @param s the target subscriber
     */
    public static void complete(Subscriber<?> s) {
        s.onSubscribe(INSTANCE);
        s.onComplete();
    }

    @Override
    public void request(long n) {
        SubscriptionHelper.validate(n);
    }

    @Override
    public void cancel() {
        // no-op
    }

    @Override
    public String toString() {
        return "EmptySubscription";
    }

    @Nullable
    @Override
    public Object poll() {
        return null; // always empty
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public void clear() {
        // nothing to do
    }

    @Override
    public int requestFusion(int mode) {
        return mode & ASYNC; // accept async mode: an onComplete or onError will be signalled after anyway
    }

    @Override
    public boolean offer(Object value) {
        throw new UnsupportedOperationException("Should not be called!");
    }

    @Override
    public boolean offer(Object v1, Object v2) {
        throw new UnsupportedOperationException("Should not be called!");
    }
}
