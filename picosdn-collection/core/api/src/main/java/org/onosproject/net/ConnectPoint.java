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
package org.onosproject.net;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.UUID;

import org.onosproject.security.ProvEntity;

/**
 * Abstraction of a network connection point expressed as a pair of the network
 * element identifier and port number.
 */
public class ConnectPoint implements Comparable<ConnectPoint>, ProvEntity {

    private final ElementId elementId;
    private final PortNumber portNumber;
    private UUID entityUuid;

    /**
     * Creates a new connection point.
     *
     * @param elementId  network element identifier
     * @param portNumber port number
     */
    public ConnectPoint(ElementId elementId, PortNumber portNumber) {
        this.elementId = elementId;
        this.portNumber = portNumber;
    }

    /**
     * Returns the network element identifier.
     *
     * @return element identifier
     */
    public ElementId elementId() {
        return elementId;
    }

    /**
     * Returns the identifier of the infrastructure device if the connection
     * point belongs to a network element which is indeed an infrastructure
     * device.
     *
     * @return network element identifier as a device identifier
     * @throws java.lang.IllegalStateException if connection point is not
     *                                         associated with a device
     */
    public DeviceId deviceId() {
        if (elementId instanceof DeviceId) {
            return (DeviceId) elementId;
        }
        throw new IllegalStateException("Connection point not associated "
                + "with an infrastructure device");
    }

    /**
     * Returns the identifier of the infrastructure device if the connection
     * point belongs to a network element which is indeed an end-station host.
     *
     * @return network element identifier as a host identifier
     * @throws java.lang.IllegalStateException if connection point is not
     *                                         associated with a host
     */
    public HostId hostId() {
        if (elementId instanceof HostId) {
            return (HostId) elementId;
        }
        throw new IllegalStateException("Connection point not associated "
                + "with an end-station host");
    }

    /**
     * Returns the identifier of the infrastructure device if the connection
     * point belongs to a network element which is indeed an ip of pcc client
     * identifier.
     *
     * @return network element identifier as a pcc client identifier
     * @throws java.lang.IllegalStateException if connection point is not
     *                                         associated with a pcc client
     */
    public IpElementId ipElementId() {
        if (elementId instanceof IpElementId) {
            return (IpElementId) elementId;
        }
        throw new IllegalStateException(
                "Connection point not associated " + "with an pcc client");
    }

    /**
     * Returns the connection port number.
     *
     * @return port number
     */
    public PortNumber port() {
        return portNumber;
    }

    /**
     * Parse a device connect point from a string. The connect point should be
     * in the format "deviceUri/portNumber".
     *
     * @param string string to parse
     * @return a ConnectPoint based on the information in the string.
     */
    public static ConnectPoint deviceConnectPoint(String string) {
        checkNotNull(string);
        String[] splitted = string.split("/");
        checkArgument(splitted.length == 2,
                "Connect point must be in \"deviceUri/portNumber\" format");

        return new ConnectPoint(DeviceId.deviceId(splitted[0]),
                PortNumber.portNumber(splitted[1]));
    }

    /**
     * Parse a host connect point from a string. The connect point should be in
     * the format "hostId/vlanId/portNumber".
     *
     * @param string string to parse
     * @return a ConnectPoint based on the information in the string.
     */
    public static ConnectPoint hostConnectPoint(String string) {
        checkNotNull(string);
        String[] splitted = string.split("/");
        checkArgument(splitted.length == 3,
                "Connect point must be in \"hostId/vlanId/portNumber\" format");

        int lastSlash = string.lastIndexOf("/");

        return new ConnectPoint(HostId.hostId(string.substring(0, lastSlash)),
                PortNumber.portNumber(
                        string.substring(lastSlash + 1, string.length())));
    }

    /**
     * Parse a device connect point from a string. The connect point should be
     * in the same format as toString output.
     *
     * @param string string to parse
     * @return a ConnectPoint based on the information in the string.
     */
    public static ConnectPoint fromString(String string) {
        checkNotNull(string);
        String[] splitted = string.split("/");
        checkArgument(splitted.length == 2,
                "Connect point must be in \"deviceUri/portNumber\" format");

        return new ConnectPoint(DeviceId.deviceId(splitted[0]),
                PortNumber.fromString(splitted[1]));
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId, portNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ConnectPoint) {
            final ConnectPoint other = (ConnectPoint) obj;
            return Objects.equals(this.elementId, other.elementId)
                    && Objects.equals(this.portNumber, other.portNumber);
        }
        return false;
    }

    @Override
    public String toString() {
        return elementId + "/" + portNumber;
    }

    @Override
    public int compareTo(ConnectPoint o) {
        int result = deviceId().toString().compareTo(o.deviceId().toString());
        if (result == 0) {
            long delta = port().toLong() - o.port().toLong();
            result = delta == 0 ? 0 : delta < 0 ? -1 : +1;
        }
        return result;
    }

    @Override
    public UUID getEntityUuid() {
        return entityUuid;
    }

    @Override
    public void setEntityUuid(UUID uuid) {
        entityUuid = uuid;
    }
}
