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

package yourpck.reactivex.internal.observers;

import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import yourpck.reactivex.Observer;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.internal.disposables.DisposableHelper;
import yourpck.reactivex.internal.util.BlockingHelper;
import yourpck.reactivex.plugins.RxJavaPlugins;

import static yourpck.reactivex.internal.util.ExceptionHelper.timeoutMessage;

/**
 * An Observer + Future that expects exactly one upstream value and provides it
 * via the (blocking) Future API.
 *
 * @param <T> the value type
 */
public final class FutureObserver<T> extends CountDownLatch
        implements Observer<T>, Future<T>, Disposable {

    final AtomicReference<Disposable> upstream;
    T value;
    Throwable error;

    public FutureObserver() {
        super(1);
        this.upstream = new AtomicReference<Disposable>();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        for (; ; ) {
            Disposable a = upstream.get();
            if (a == this || a == DisposableHelper.DISPOSED) {
                return false;
            }

            if (upstream.compareAndSet(a, DisposableHelper.DISPOSED)) {
                if (a != null) {
                    a.dispose();
                }
                countDown();
                return true;
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return DisposableHelper.isDisposed(upstream.get());
    }

    @Override
    public boolean isDone() {
        return getCount() == 0;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (getCount() != 0) {
            BlockingHelper.verifyNonBlocking();
            await();
        }

        if (isCancelled()) {
            throw new CancellationException();
        }
        Throwable ex = error;
        if (ex != null) {
            throw new ExecutionException(ex);
        }
        return value;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (getCount() != 0) {
            BlockingHelper.verifyNonBlocking();
            if (!await(timeout, unit)) {
                throw new TimeoutException(timeoutMessage(timeout, unit));
            }
        }

        if (isCancelled()) {
            throw new CancellationException();
        }

        Throwable ex = error;
        if (ex != null) {
            throw new ExecutionException(ex);
        }
        return value;
    }

    @Override
    public void onSubscribe(Disposable d) {
        DisposableHelper.setOnce(this.upstream, d);
    }

    @Override
    public void onNext(T t) {
        if (value != null) {
            upstream.get().dispose();
            onError(new IndexOutOfBoundsException("More than one element received"));
            return;
        }
        value = t;
    }

    @Override
    public void onError(Throwable t) {
        if (error == null) {
            error = t;

            for (; ; ) {
                Disposable a = upstream.get();
                if (a == this || a == DisposableHelper.DISPOSED) {
                    RxJavaPlugins.onError(t);
                    return;
                }
                if (upstream.compareAndSet(a, this)) {
                    countDown();
                    return;
                }
            }
        } else {
            RxJavaPlugins.onError(t);
        }
    }

    @Override
    public void onComplete() {
        if (value == null) {
            onError(new NoSuchElementException("The source is empty"));
            return;
        }
        for (; ; ) {
            Disposable a = upstream.get();
            if (a == this || a == DisposableHelper.DISPOSED) {
                return;
            }
            if (upstream.compareAndSet(a, this)) {
                countDown();
                return;
            }
        }
    }

    @Override
    public void dispose() {
        // ignoring as `this` means a finished Disposable only
    }

    @Override
    public boolean isDisposed() {
        return isDone();
    }
}
