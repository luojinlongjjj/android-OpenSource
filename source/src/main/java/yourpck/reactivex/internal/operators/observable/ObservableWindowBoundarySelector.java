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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.Observable;
import yourpck.reactivex.ObservableSource;
import yourpck.reactivex.Observer;
import yourpck.reactivex.disposables.CompositeDisposable;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.exceptions.Exceptions;
import yourpck.reactivex.functions.Function;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.functions.ObjectHelper;
import yourpck.reactivex.internal.observers.QueueDrainObserver;
import yourpck.reactivex.internal.queue.MpscLinkedQueue;
import yourpck.reactivex.internal.util.NotificationLite;
import yourpck.reactivex.observers.DisposableObserver;
import yourpck.reactivex.observers.SerializedObserver;
import yourpck.reactivex.plugins.RxJavaPlugins;
import yourpck.reactivex.subjects.UnicastSubject;

public final class ObservableWindowBoundarySelector<T, B, V> extends AbstractObservableWithUpstream<T, Observable<T>> {
    final ObservableSource<B> open;
    final Function<? super B, ? extends ObservableSource<V>> close;
    final int bufferSize;

    public ObservableWindowBoundarySelector(
            ObservableSource<T> source,
            ObservableSource<B> open, Function<? super B, ? extends ObservableSource<V>> close,
            int bufferSize) {
        super(source);
        this.open = open;
        this.close = close;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribeActual(Observer<? super Observable<T>> t) {
        source.subscribe(new WindowBoundaryMainObserver<T, B, V>(
                new SerializedObserver<Observable<T>>(t),
                open, close, bufferSize));
    }

    static final class WindowBoundaryMainObserver<T, B, V>
            extends QueueDrainObserver<T, Object, Observable<T>>
            implements Disposable {
        final ObservableSource<B> open;
        final Function<? super B, ? extends ObservableSource<V>> close;
        final int bufferSize;
        final CompositeDisposable resources;
        final AtomicReference<Disposable> boundary = new AtomicReference<Disposable>();
        final List<UnicastSubject<T>> ws;
        final AtomicLong windows = new AtomicLong();
        final AtomicBoolean stopWindows = new AtomicBoolean();
        Disposable upstream;

        WindowBoundaryMainObserver(Observer<? super Observable<T>> actual,
                                   ObservableSource<B> open, Function<? super B, ? extends ObservableSource<V>> close, int bufferSize) {
            super(actual, new MpscLinkedQueue<Object>());
            this.open = open;
            this.close = close;
            this.bufferSize = bufferSize;
            this.resources = new CompositeDisposable();
            this.ws = new ArrayList<UnicastSubject<T>>();
            windows.lazySet(1);
        }

        @Override
        public void onSubscribe(Disposable d) {
            if (DisposableHelper.validate(this.upstream, d)) {
                this.upstream = d;

                downstream.onSubscribe(this);

                if (stopWindows.get()) {
                    return;
                }

                OperatorWindowBoundaryOpenObserver<T, B> os = new OperatorWindowBoundaryOpenObserver<T, B>(this);

                if (boundary.compareAndSet(null, os)) {
                    open.subscribe(os);
                }
            }
        }

        @Override
        public void onNext(T t) {
            if (fastEnter()) {
                for (UnicastSubject<T> w : ws) {
                    w.onNext(t);
                }
                if (leave(-1) == 0) {
                    return;
                }
            } else {
                queue.offer(NotificationLite.next(t));
                if (!enter()) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            error = t;
            done = true;

            if (enter()) {
                drainLoop();
            }

            if (windows.decrementAndGet() == 0) {
                resources.dispose();
            }

            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            if (enter()) {
                drainLoop();
            }

            if (windows.decrementAndGet() == 0) {
                resources.dispose();
            }

            downstream.onComplete();
        }

        void error(Throwable t) {
            upstream.dispose();
            resources.dispose();
            onError(t);
        }

        @Override
        public void dispose() {
            if (stopWindows.compareAndSet(false, true)) {
                DisposableHelper.dispose(boundary);
                if (windows.decrementAndGet() == 0) {
                    upstream.dispose();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return stopWindows.get();
        }

        void disposeBoundary() {
            resources.dispose();
            DisposableHelper.dispose(boundary);
        }

        void drainLoop() {
            final MpscLinkedQueue<Object> q = (MpscLinkedQueue<Object>) queue;
            final Observer<? super Observable<T>> a = downstream;
            final List<UnicastSubject<T>> ws = this.ws;
            int missed = 1;

            for (; ; ) {

                for (; ; ) {
                    boolean d = done;

                    Object o = q.poll();

                    boolean empty = o == null;

                    if (d && empty) {
                        disposeBoundary();
                        Throwable e = error;
                        if (e != null) {
                            for (UnicastSubject<T> w : ws) {
                                w.onError(e);
                            }
                        } else {
                            for (UnicastSubject<T> w : ws) {
                                w.onComplete();
                            }
                        }
                        ws.clear();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    if (o instanceof WindowOperation) {
                        @SuppressWarnings("unchecked")
                        WindowOperation<T, B> wo = (WindowOperation<T, B>) o;

                        UnicastSubject<T> w = wo.w;
                        if (w != null) {
                            if (ws.remove(wo.w)) {
                                wo.w.onComplete();

                                if (windows.decrementAndGet() == 0) {
                                    disposeBoundary();
                                    return;
                                }
                            }
                            continue;
                        }

                        if (stopWindows.get()) {
                            continue;
                        }

                        w = UnicastSubject.create(bufferSize);

                        ws.add(w);
                        a.onNext(w);

                        ObservableSource<V> p;

                        try {
                            p = ObjectHelper.requireNonNull(close.apply(wo.open), "The ObservableSource supplied is null");
                        } catch (Throwable e) {
                            Exceptions.throwIfFatal(e);
                            stopWindows.set(true);
                            a.onError(e);
                            continue;
                        }

                        OperatorWindowBoundaryCloseObserver<T, V> cl = new OperatorWindowBoundaryCloseObserver<T, V>(this, w);

                        if (resources.add(cl)) {
                            windows.getAndIncrement();

                            p.subscribe(cl);
                        }

                        continue;
                    }

                    for (UnicastSubject<T> w : ws) {
                        w.onNext(NotificationLite.<T>getValue(o));
                    }
                }

                missed = leave(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void accept(Observer<? super Observable<T>> a, Object v) {
        }

        void open(B b) {
            queue.offer(new WindowOperation<Object, B>(null, b));
            if (enter()) {
                drainLoop();
            }
        }

        void close(OperatorWindowBoundaryCloseObserver<T, V> w) {
            resources.delete(w);
            queue.offer(new WindowOperation<T, Object>(w.w, null));
            if (enter()) {
                drainLoop();
            }
        }
    }

    static final class WindowOperation<T, B> {
        final UnicastSubject<T> w;
        final B open;

        WindowOperation(UnicastSubject<T> w, B open) {
            this.w = w;
            this.open = open;
        }
    }

    static final class OperatorWindowBoundaryOpenObserver<T, B> extends DisposableObserver<B> {
        final WindowBoundaryMainObserver<T, B, ?> parent;

        OperatorWindowBoundaryOpenObserver(WindowBoundaryMainObserver<T, B, ?> parent) {
            this.parent = parent;
        }

        @Override
        public void onNext(B t) {
            parent.open(t);
        }

        @Override
        public void onError(Throwable t) {
            parent.error(t);
        }

        @Override
        public void onComplete() {
            parent.onComplete();
        }
    }

    static final class OperatorWindowBoundaryCloseObserver<T, V> extends DisposableObserver<V> {
        final WindowBoundaryMainObserver<T, ?, V> parent;
        final UnicastSubject<T> w;

        boolean done;

        OperatorWindowBoundaryCloseObserver(WindowBoundaryMainObserver<T, ?, V> parent, UnicastSubject<T> w) {
            this.parent = parent;
            this.w = w;
        }

        @Override
        public void onNext(V t) {
            dispose();
            onComplete();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            parent.error(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            parent.close(this);
        }
    }
}
