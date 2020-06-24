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
package yourpck.reactivex;

import yourpck.reactivex.annotations.NonNull;

/**
 * Interface to map/wrap a downstream observer to an upstream observer.
 *
 * @param <Downstream> the value type of the downstream
 * @param <Upstream>   the value type of the upstream
 */
public interface MaybeOperator<Downstream, Upstream> {
    /**
     * Applies a function to the child MaybeObserver and returns a new parent MaybeObserver.
     *
     * @param observer the child MaybeObserver instance
     * @return the parent MaybeObserver instance
     * @throws Exception on failure
     */
    @NonNull
    MaybeObserver<? super Upstream> apply(@NonNull MaybeObserver<? super Downstream> observer) throws Exception;
}
