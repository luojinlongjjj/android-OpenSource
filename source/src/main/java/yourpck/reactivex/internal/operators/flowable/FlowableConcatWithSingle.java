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

import java.util.concurrent.atomic.AtomicReference;

import yourpck.org.reactivestreams.Subscriber;
import yourpck.reactivex.Flowable;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.subscribers.SinglePostCompleteSubscriber;
import yourpck.reactivex.internal.subscriptions.SubscriptionHelper;

/**
 * Subscribe to a main Flowable first, then when it completes normally, subscribe to a Single,
 * signal its success value followed by a completion or signal its error as is.
 * <p>History: 2.1.10 - experimental
 *
 * @param <T> the element type of the main source and output type
 * @since 2.2
 */
public final class FlowableConcatWithSingle<T> extends AbstractFlowableWithUpstream<T, T> {

    final SingleSource<? extends T> other;

    public FlowableConcatWithSingle(Flowable<T> source, SingleSource<? extends T> other) {
        super(source);
        this.other = other;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new ConcatWithSubscriber<T>(s, other));
    }

    static final class ConcatWithSubscriber<T>
            extends SinglePostCompleteSubscriber<T, T>
            implements SingleObserver<T> {

        private static final long serialVersionUID = -7346385463600070225L;

        final AtomicReference<Disposable> otherDisposable;

        SingleSource<? extends T> other;

        ConcatWithSubscriber(Subscriber<? super T> actual, SingleSource<? extends T> other) {
            super(actual);
            this.other = other;
            this.otherDisposable = new AtomicReference<Disposable>();
        }

        @Override
        public void onSubscribe(Disposable d) {
            DisposableHelper.setOnce(otherDisposable, d);
        }

        @Override
        public void onNext(T t) {
            produced++;
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onSuccess(T t) {
            complete(t);
        }

        @Override
        public void onComplete() {
            upstream = SubscriptionHelper.CANCELLED;
            SingleSource<? extends T> ss = other;
            other = null;
            ss.subscribe(this);
        }

        @Override
        public void cancel() {
            super.cancel();
            DisposableHelper.dispose(otherDisposable);
        }
    }
}
