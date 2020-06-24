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
package yourpck.reactivex.disposables;

import yourpck.reactivex.annotations.NonNull;
import yourpck.reactivex.functions.Action;
import yourpck.reactivex.internal.util.ExceptionHelper;

final class ActionDisposable extends ReferenceDisposable<Action> {

    private static final long serialVersionUID = -8219729196779211169L;

    ActionDisposable(Action value) {
        super(value);
    }

    @Override
    protected void onDisposed(@NonNull Action value) {
        try {
            value.run();
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }
}
