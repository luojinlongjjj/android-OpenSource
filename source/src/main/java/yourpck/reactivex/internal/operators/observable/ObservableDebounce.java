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

package yourpck.reactivex.internal.operators.observable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.ObservableSource;
import yourpck.reactivex.Observer;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.observers.DisposableObserver;
import yourpck.reactivex.observers.SerializedObserver;
import yourpck.reactivex.plugins.RxJavaPlugins;

public final class ObservableDebounce<T, U> extends AbstractObservableWithUpstream<T, T> {
    final Function<? super T, ? extends ObservableSource<U>> debounceSelector;

    public ObservableDebounce(ObservableSource<T> source, Function<? super T, ? extends ObservableSource<U>> debounceSelector) {
        super(source);
        this.debounceSelector = debounceSelector;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new DebounceObserver<T, U>(new SerializedObserver<T>(t), debounceSelector));
    }

    static final class DebounceObserver<T, U>
            implements Observer<T>, Disposable {
        final Observer<? super T> downstream;
        final Function<? super T, ? extends ObservableSource<U>> debounceSelector;
        final AtomicReference<Disposable> debouncer = new AtomicReference<Disposable>();
        Disposable upstream;
        volatile long index;

        boolean done;

        DebounceObserver(Observer<? super T> actual,
                         Function<? super T, ? extends ObservableSource<U>> debounceSelector) {
            this.downstream = actual;
            this.debounceSelector = debounceSelector;
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }

            long idx = index + 1;
            index = idx;

            Disposable d = debouncer.get();
            if (d != null) {
                d.dispose();
            }

            ObservableSource<U> p;

            try {
                p = ObjectHelper.requireNonNull(debounceSelector.apply(t), "The ObservableSource supplied is null");
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                dispose();
                downstream.onError(e);
                return;
            }

            DebounceInnerObserver<T, U> dis = new DebounceInnerObserver<T, U>(this, idx, t);

            if (debouncer.compareAndSet(d, dis)) {
                p.subscribe(dis);
            }
        }

        @Override
        public void onError(Throwable t) {
            DisposableHelper.dispose(debouncer);
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            Disposable d = debouncer.get();
            if (d != DisposableHelper.DISPOSED) {
                @SuppressWarnings("unchecked")
                DebounceInnerObserver<T, U> dis = (DebounceInnerObserver<T, U>) d;
                dis.emit();
                DisposableHelper.dispose(debouncer);
                downstream.onComplete();
            }
        }

        @Override
        public void dispose() {
            upstream.dispose();
            DisposableHelper.dispose(debouncer);
        }

        @Override
        public boolean isDisposed() {
            return upstream.isDisposed();
        }

        void emit(long idx, T value) {
            if (idx == index) {
                downstream.onNext(value);
            }
        }

        static final class DebounceInnerObserver<T, U> extends DisposableObserver<U> {
            final DebounceObserver<T, U> parent;
            final long index;
            final T value;
            final AtomicBoolean once = new AtomicBoolean();
            boolean done;

            DebounceInnerObserver(DebounceObserver<T, U> parent, long index, T value) {
                this.parent = parent;
                this.index = index;
                this.value = value;
            }

            @Override
            public void onNext(U t) {
                if (done) {
                    return;
                }
                done = true;
                dispose();
                emit();
            }

            void emit() {
                if (once.compareAndSet(false, true)) {
                    parent.emit(index, value);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (done) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                done = true;
                parent.onError(t);
            }

            @Override
            public void onComplete() {
                if (done) {
                    return;
                }
                done = true;
                emit();
            }
        }
    }
}
