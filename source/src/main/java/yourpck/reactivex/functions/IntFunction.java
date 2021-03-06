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
package yourpck.reactivex.functions;

import yourpck.reactivex.annotations.NonNull;

/**
 * A functional interface (callback) that takes a primitive value and return value of type T.
 *
 * @param <T> the returned value type
 */
public interface IntFunction<T> {
    /**
     * Calculates a value based on a primitive integer input.
     *
     * @param i the input value
     * @return the result Object
     * @throws Exception on error
     */
    @NonNull
    T apply(int i) throws Exception;
}
