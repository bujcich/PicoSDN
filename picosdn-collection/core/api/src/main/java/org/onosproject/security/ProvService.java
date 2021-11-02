/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.security;

public interface ProvService {

    public void recordDispatch(ProvActivity activity);

    public void recordListen(ProvActivity activity, Class listener);

    public void recordApiCall(ProvApiCallType type, ProvEntity entity,
            ProvEntity afterUpdateEntity, Throwable location);

    public void recordDerivation(ProvEntity child, ProvEntity parent);

}
