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
package yourpck.reactivex.observers;

import yourpck.reactivex.Observer;
import yourpck.reactivex.annotations.NonNull;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.util.AppendOnlyLinkedArrayList;
import yourpck.reactivex.internal.util.NotificationLite;
import yourpck.reactivex.plugins.RxJavaPlugins;

/**
 * Serializes access to the onNext, onError and onComplete methods of another Observer.
 *
 * <p>Note that {@link #onSubscribe(Disposable)} is not serialized in respect of the other methods so
 * make sure the {@code onSubscribe()} is called with a non-null {@code Disposable}
 * before any of the other methods are called.
 *
 * <p>The implementation assumes that the actual Observer's methods don't throw.
 *
 * @param <T> the value type
 */
public final class SerializedObserver<T> implements Observer<T>, Disposable {
    static final int QUEUE_LINK_SIZE = 4;
    final Observer<? super T> downstream;
    final boolean delayError;
    Disposable upstream;

    boolean emitting;
    AppendOnlyLinkedArrayList<Object> queue;

    volatile boolean done;

    /**
     * Construct a SerializedObserver by wrapping the given actual Observer.
     *
     * @param downstream the actual Observer, not null (not verified)
     */
    public SerializedObserver(@NonNull Observer<? super T> downstream) {
        this(downstream, false);
    }

    /**
     * Construct a SerializedObserver by wrapping the given actual Observer and
     * optionally delaying the errors till all regular values have been emitted
     * from the internal buffer.
     *
     * @param actual     the actual Observer, not null (not verified)
     * @param delayError if true, errors are emitted after regular values have been emitted
     */
    public SerializedObserver(@NonNull Observer<? super T> actual, boolean delayError) {
        this.downstream = actual;
        this.delayError = delayError;
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        if (DisposableHelper.validate(this.upstream, d)) {
            this.upstream = d;

            downstream.onSubscribe(this);
        }
    }

    @Override
    public void dispose() {
        upstream.dispose();
    }

    @Override
    public boolean isDisposed() {
        return upstream.isDisposed();
    }

    @Override
    public void onNext(@NonNull T t) {
        if (done) {
            return;
        }
        if (t == null) {
            upstream.dispose();
            onError(new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
            return;
        }
        synchronized (this) {
            if (done) {
                return;
            }
            if (emitting) {
                AppendOnlyLinkedArrayList<Object> q = queue;
                if (q == null) {
                    q = new AppendOnlyLinkedArrayList<Object>(QUEUE_LINK_SIZE);
                    queue = q;
                }
                q.add(NotificationLite.next(t));
                return;
            }
            emitting = true;
        }

        downstream.onNext(t);

        emitLoop();
    }

    @Override
    public void onError(@NonNull Throwable t) {
        if (done) {
            RxJavaPlugins.onError(t);
            return;
        }
        boolean reportError;
        synchronized (this) {
            if (done) {
                reportError = true;
            } else if (emitting) {
                done = true;
                AppendOnlyLinkedArrayList<Object> q = queue;
                if (q == null) {
                    q = new AppendOnlyLinkedArrayList<Object>(QUEUE_LINK_SIZE);
                    queue = q;
                }
                Object err = NotificationLite.error(t);
                if (delayError) {
                    q.add(err);
                } else {
                    q.setFirst(err);
                }
                return;
            } else {
                done = true;
                emitting = true;
                reportError = false;
            }
        }

        if (reportError) {
            RxJavaPlugins.onError(t);
            return;
        }

        downstream.onError(t);
        // no need to loop because this onError is the last event
    }

    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        synchronized (this) {
            if (done) {
                return;
            }
            if (emitting) {
                AppendOnlyLinkedArrayList<Object> q = queue;
                if (q == null) {
                    q = new AppendOnlyLinkedArrayList<Object>(QUEUE_LINK_SIZE);
                    queue = q;
                }
                q.add(NotificationLite.complete());
                return;
            }
            done = true;
            emitting = true;
        }

        downstream.onComplete();
        // no need to loop because this onComplete is the last event
    }

    void emitLoop() {
        for (; ; ) {
            AppendOnlyLinkedArrayList<Object> q;
            synchronized (this) {
                q = queue;
                if (q == null) {
                    emitting = false;
                    return;
                }
                queue = null;
            }

            if (q.accept(downstream)) {
                return;
            }
        }
    }
}
