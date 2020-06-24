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

import yourpck.org.reactivestreams.Publisher;
import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.internal.operators.flowable.FlowableTake.TakeSubscriber;

/**
 * Take with a generic Publisher source.
 * <p>History: 2.0.7 - experimental
 *
 * @param <T> the value type
 * @since 2.1
 */
public final class FlowableTakePublisher<T> extends Flowable<T> {

    final Publisher<T> source;
    final long limit;

    public FlowableTakePublisher(Publisher<T> source, long limit) {
        this.source = source;
        this.limit = limit;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new TakeSubscriber<T>(s, limit));
    }
}
