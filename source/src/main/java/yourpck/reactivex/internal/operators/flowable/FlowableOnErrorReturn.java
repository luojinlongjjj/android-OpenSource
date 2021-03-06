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
import yourpck.reactivex.Flowable;
import yourpck.reactivex.exceptions.CompositeException;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.subscribers.SinglePostCompleteSubscriber;

public final class FlowableOnErrorReturn<T> extends AbstractFlowableWithUpstream<T, T> {
    final Function<? super Throwable, ? extends T> valueSupplier;

    public FlowableOnErrorReturn(Flowable<T> source, Function<? super Throwable, ? extends T> valueSupplier) {
        super(source);
        this.valueSupplier = valueSupplier;
    }

    @Override
    protected void subscribeActual(Subscriber<? super T> s) {
        source.subscribe(new OnErrorReturnSubscriber<T>(s, valueSupplier));
    }

    static final class OnErrorReturnSubscriber<T>
            extends SinglePostCompleteSubscriber<T, T> {

        private static final long serialVersionUID = -3740826063558713822L;
        final Function<? super Throwable, ? extends T> valueSupplier;

        OnErrorReturnSubscriber(Subscriber<? super T> actual, Function<? super Throwable, ? extends T> valueSupplier) {
            super(actual);
            this.valueSupplier = valueSupplier;
        }

        @Override
        public void onNext(T t) {
            produced++;
            downstream.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            T v;
            try {
                v = ObjectHelper.requireNonNull(valueSupplier.apply(t), "The valueSupplier returned a null value");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(t, ex));
                return;
            }
            complete(v);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
