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

import yourpck.reactivex.Observable;
import yourpck.reactivex.ObservableSource;
import yourpck.reactivex.internal.fuseable.HasUpstreamObservableSource;

/**
 * Base class for operators with a source consumable.
 *
 * @param <T> the input source type
 * @param <U> the output type
 */
abstract class AbstractObservableWithUpstream<T, U> extends Observable<U> implements HasUpstreamObservableSource<T> {

    /**
     * The source consumable Observable.
     */
    protected final ObservableSource<T> source;

    /**
     * Constructs the ObservableSource with the given consumable.
     *
     * @param source the consumable Observable
     */
    AbstractObservableWithUpstream(ObservableSource<T> source) {
        this.source = source;
    }

    @Override
    public final ObservableSource<T> source() {
        return source;
    }

}
