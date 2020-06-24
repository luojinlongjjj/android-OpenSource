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

package yourpck.reactivex.internal.operators.single;

import yourpck.reactivex.Single;
import yourpck.reactivex.SingleObserver;
import yourpck.reactivex.SingleSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Consumer;
import yourpck.reactivex.internal.disposables.EmptyDisposable;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Calls a callback when the upstream calls onSubscribe with a disposable.
 *
 * @param <T> the value type
 */
public final class SingleDoOnSubscribe<T> extends Single<T> {

    final SingleSource<T> source;

    final Consumer<? super Disposable> onSubscribe;

    public SingleDoOnSubscribe(SingleSource<T> source, Consumer<? super Disposable> onSubscribe) {
        this.source = source;
        this.onSubscribe = onSubscribe;
    }

    @Override
    protected void subscribeActual(final SingleObserver<? super T> observer) {
        source.subscribe(new DoOnSubscribeSingleObserver<T>(observer, onSubscribe));
    }

    static final class DoOnSubscribeSingleObserver<T> implements SingleObserver<T> {

        final SingleObserver<? super T> downstream;

        final Consumer<? super Disposable> onSubscribe;

        boolean done;

        DoOnSubscribeSingleObserver(SingleObserver<? super T> actual, Consumer<? super Disposable> onSubscribe) {
            this.downstream = actual;
            this.onSubscribe = onSubscribe;
        }

        @Override
        public void onSubscribe(Disposable d) {
            try {
                onSubscribe.accept(d);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                done = true;
                d.dispose();
                EmptyDisposable.error(ex, downstream);
                return;
            }

            downstream.onSubscribe(d);
        }

        @Override
        public void onSuccess(T value) {
            if (done) {
                return;
            }
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            downstream.onError(e);
        }
    }

}
