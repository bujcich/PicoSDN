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

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;

public final class ProvHook {
    private static ServiceDirectory services = new DefaultServiceDirectory();
    private static ProvService svc = null;

    /* Prevent instantiation */
    private ProvHook() {
    }

    /* Start sending provenance to ProvService, if enabled */
    public static void start() {
        try {
            svc = services.get(ProvService.class);
        } catch (Exception e) {
            svc = null;
        }
    }

    /* Stop sending provenance to ProvService */
    public static void stop() {
        svc = null;
    }

    /* Record a dispatch at the start of an activity */
    public static void recordDispatch(ProvActivity activity)
            throws RuntimeException {
        if (svc == null) {
            return;
        }
        svc.recordDispatch(activity);
    }

    /* Record the start of a listen (dispatch must be called first) */
    public static void recordListen(ProvActivity activity, Class listener)
            throws RuntimeException {
        if (svc == null) {
            return;
        }
        svc.recordListen(activity, listener);
    }

    /* Record an API call (no entity) */
    public static void recordApiCall(ProvApiCallType type)
            throws RuntimeException {
        if (svc == null) {
            return;
        }
        Throwable location = new Throwable();
        svc.recordApiCall(type, null, null, location);
    }

    /* Record an API call (single create, read, delete) */
    public static void recordApiCall(ProvApiCallType type, ProvEntity entity)
            throws RuntimeException {
        if (svc == null) {
            return;
        }
        Throwable location = new Throwable();
        svc.recordApiCall(type, entity, null, location);
    }

    /* Record an API call (iterable creates, reads, deletes) */
    public static void recordApiCall(ProvApiCallType type,
            Iterable<? extends ProvEntity> entities) throws RuntimeException {
        if (svc == null) {
            return;
        }
        Throwable location = new Throwable();
        for (ProvEntity entity : entities) {
            svc.recordApiCall(type, entity, null, location);
        }
    }

    /* Record an API call (single create, read, update, delete) */
    public static void recordApiCall(ProvApiCallType type, ProvEntity entity,
            ProvEntity afterUpdateEntity) throws RuntimeException {
        if (svc == null) {
            return;
        }
        Throwable location = new Throwable();
        svc.recordApiCall(type, entity, afterUpdateEntity, location);
    }

    /* Record a derivation (one object derived from one parent object) */
    public static void recordDerivation(ProvEntity child, ProvEntity parent)
            throws RuntimeException {
        if (svc == null) {
            return;
        }
        svc.recordDerivation(child, parent);
    }

    /* Record a derivation (many objects derived from one parent object) */
    public static void recordDerivation(Iterable<? extends ProvEntity> children,
            ProvEntity parent) throws RuntimeException {
        if (svc == null) {
            return;
        }
        for (ProvEntity child : children) {
            svc.recordDerivation(child, parent);
        }
    }

}
