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

package yourpck.reactivex.internal.operators.maybe;

import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.MaybeObserver;
import yourpck.reactivex.MaybeSource;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.CompositeException;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;

/**
 * Subscribes to the MaybeSource returned by a function if the main source signals an onError.
 *
 * @param <T> the value type
 */
public final class MaybeOnErrorNext<T> extends AbstractMaybeWithUpstream<T, T> {

    final Function<? super Throwable, ? extends MaybeSource<? extends T>> resumeFunction;

    final boolean allowFatal;

    public MaybeOnErrorNext(MaybeSource<T> source,
                            Function<? super Throwable, ? extends MaybeSource<? extends T>> resumeFunction,
                            boolean allowFatal) {
        super(source);
        this.resumeFunction = resumeFunction;
        this.allowFatal = allowFatal;
    }

    @Override
    protected void subscribeActual(MaybeObserver<? super T> observer) {
        source.subscribe(new OnErrorNextMaybeObserver<T>(observer, resumeFunction, allowFatal));
    }

    static final class OnErrorNextMaybeObserver<T>
            extends AtomicReference<Disposable>
            implements MaybeObserver<T>, Disposable {

        private static final long serialVersionUID = 2026620218879969836L;

        final MaybeObserver<? super T> downstream;

        final Function<? super Throwable, ? extends MaybeSource<? extends T>> resumeFunction;

        final boolean allowFatal;

        OnErrorNextMaybeObserver(MaybeObserver<? super T> actual,
                                 Function<? super Throwable, ? extends MaybeSource<? extends T>> resumeFunction,
                                 boolean allowFatal) {
            this.downstream = actual;
            this.resumeFunction = resumeFunction;
            this.allowFatal = allowFatal;
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.setOnce(this, d)) {
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onSuccess(T value) {
            downstream.onSuccess(value);
        }

        @Override
        public void onError(Throwable e) {
            if (!allowFatal && !(e instanceof Exception)) {
                downstream.onError(e);
                return;
            }
            MaybeSource<? extends T> m;

            try {
                m = ObjectHelper.requireNonNull(resumeFunction.apply(e), "The resumeFunction returned a null MaybeSource");
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                downstream.onError(new CompositeException(e, ex));
                return;
            }

            DisposableHelper.replace(this, null);

            m.subscribe(new NextMaybeObserver<T>(downstream, this));
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }

        static final class NextMaybeObserver<T> implements MaybeObserver<T> {
            final MaybeObserver<? super T> downstream;

            final AtomicReference<Disposable> upstream;

            NextMaybeObserver(MaybeObserver<? super T> actual, AtomicReference<Disposable> d) {
                this.downstream = actual;
                this.upstream = d;
            }

            @Override
            public void onSubscribe(Disposable d) {
                DisposableHelper.setOnce(this.upstream, d);
            }

            @Override
            public void onSuccess(T value) {
                downstream.onSuccess(value);
            }

            @Override
            public void onError(Throwable e) {
                downstream.onError(e);
            }

            @Override
            public void onComplete() {
                downstream.onComplete();
            }
        }
    }
}