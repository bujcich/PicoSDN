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

import java.util.Set;

import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.security.ProvEntity;

/**
 * Abstraction of an end-station host on the network, essentially a NIC.
 */
public interface Host extends Element, ProvEntity {

    /**
     * Host identification.
     *
     * @return host id
     */
    @Override
    HostId id();

    /**
     * Returns the host MAC address.
     *
     * @return mac address
     */
    MacAddress mac();

    /**
     * Returns the VLAN ID tied to this host.
     *
     * @return VLAN ID value
     */
    VlanId vlan();

    /**
     * Returns set of IP addresses currently bound to the host MAC address.
     *
     * @return set of IP addresses; empty if no IP address is bound
     */
    Set<IpAddress> ipAddresses();

    /**
     * Returns the most recent host location where the host attaches to the
     * network edge.
     *
     * @return the most recent host location
     */
    HostLocation location();

    /**
     * Returns all host locations where the host attaches to the network edge.
     *
     * @return all host locations
     */
    Set<HostLocation> locations();

    /**
     * Returns true if configured by NetworkConfiguration.
     * 
     * @return configured/learnt dynamically
     */
    default boolean configured() {
        return false;
    }

    /**
     * Returns the inner VLAN ID tied to this host.
     *
     * @return VLAN ID value; VlanId.NONE if only one VLAN ID is tied to this
     *         host
     */
    default VlanId innerVlan() {
        return VlanId.NONE;
    }

    /**
     * Returns the TPID of the outermost VLAN associated with this host.
     *
     * @return TPID of the outermost VLAN header
     */
    default EthType tpid() {
        return EthType.EtherType.UNKNOWN.ethType();
    }
    // TODO: explore capturing list of recent locations to aid in mobility

}
