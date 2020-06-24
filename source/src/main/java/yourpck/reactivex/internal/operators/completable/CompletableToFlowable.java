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

package yourpck.reactivex.internal.operators.completable;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.CompletableSource;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.internal.observers.SubscriberCompletableObserver;

public final class CompletableToFlowable<T> extends Flowable<T> {

    final CompletableSource source;

    public CompletableToFlowable(CompletableSource source) {
        this.source = source;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        SubscriberCompletableObserver<T> os = new SubscriberCompletableObserver<T>(s);
        source.subscribe(os);
    }
}
