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

import java.util.concurrent.CountDownLatch;

import yourpck.reactivex.functions.Action;
import yourpck.reactivex.functions.Consumer;

/**
 * Stores an incoming Throwable (if any) and counts itself down.
 */
public final class BlockingIgnoringReceiver
        extends CountDownLatch
        implements Consumer<Throwable>, Action {
    public Throwable error;

    public BlockingIgnoringReceiver() {
        super(1);
    }

    @Override
    public void accept(Throwable e) {
        error = e;
        countDown();
    }

    @Override
    public void run() {
        countDown();
    }
}
