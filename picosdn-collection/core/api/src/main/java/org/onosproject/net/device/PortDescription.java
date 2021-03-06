/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.device;

import org.onosproject.net.Description;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;
import org.onosproject.security.ProvEntity;

/**
 * Information about a port.
 */
public interface PortDescription extends Description, ProvEntity {

    /**
     * Returns the port number.
     *
     * @return port number
     */
    PortNumber portNumber();

    /**
     * Indicates whether or not the port is up and active.
     *
     * @return true if the port is active and has carrier signal
     */
    boolean isEnabled();

    /**
     * Indicates whether or not the port was removed.
     *
     * @return true if the port is removed.
     */
    boolean isRemoved();

    /**
     * Returns the port type.
     *
     * @return port type
     */
    Type type();

    /**
     * Returns the current port speed in Mbps.
     *
     * @return current port speed
     */
    long portSpeed();

}
