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

package yourpck.reactivex.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import yourpck.reactivex.functions.Function;

public enum ArrayListSupplier implements Callable<List<Object>>, Function<Object, List<Object>> {
    INSTANCE;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> Callable<List<T>> asCallable() {
        return (Callable) INSTANCE;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T, O> Function<O, List<T>> asFunction() {
        return (Function) INSTANCE;
    }

    @Override
    public List<Object> call() throws Exception {
        return new ArrayList<Object>();
    }

    @Override
    public List<Object> apply(Object o) throws Exception {
        return new ArrayList<Object>();
    }
}
