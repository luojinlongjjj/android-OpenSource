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

package yourpck.reactivex.internal.disposables;

import yourpck.reactivex.annotations.Experimental;
import yourpck.reactivex.disposables.Disposable;
import yourpck.reactivex.flowables.ConnectableFlowable;
import yourpck.reactivex.observables.ConnectableObservable;

/**
 * Interface allowing conditional resetting of connections in {@link ConnectableObservable}s
 * and {@link ConnectableFlowable}s.
 *
 * @since 2.2.2 - experimental
 */
@Experimental
public interface ResettableConnectable {

    /**
     * Reset the connectable source only if the given {@link Disposable} {@code connection} instance
     * is still representing a connection established by a previous {@code connect()} connection.
     * <p>
     * For example, an immediately previous connection should reset the connectable source:
     * <pre><code>
     * Disposable d = connectable.connect();
     *
     * ((ResettableConnectable)connectable).resetIf(d);
     * </code></pre>
     * However, if the connection indicator {@code Disposable} is from a much earlier connection,
     * it should not affect the current connection:
     * <pre><code>
     * Disposable d1 = connectable.connect();
     * d.dispose();
     *
     * Disposable d2 = connectable.connect();
     *
     * ((ResettableConnectable)connectable).resetIf(d);
     *
     * assertFalse(d2.isDisposed());
     * </code></pre>
     *
     * @param connection the disposable received from a previous {@code connect()} call.
     */
    void resetIf(Disposable connection);
}
