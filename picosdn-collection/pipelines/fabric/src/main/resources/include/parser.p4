/*
 * Copyright 2017-present Open Networking Foundation
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

#ifndef __PARSER__
#define __PARSER__

#include "define.p4"

parser FabricParser (
packet_in packet,
out parsed_headers_t hdr,
inout fabric_metadata_t fabric_metadata,
inout standard_metadata_t standard_metadata) {

    state start {
        transition select(standard_metadata.ingress_port) {
            CPU_PORT: parse_packet_out;
            default: parse_ethernet;
        }
    }

    state parse_packet_out {
        packet.extract(hdr.packet_out);
        transition parse_ethernet;
    }

    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.ether_type){
            ETHERTYPE_VLAN: parse_vlan_tag;
            ETHERTYPE_MPLS: parse_mpls;
            ETHERTYPE_ARP: parse_arp;
            ETHERTYPE_IPV4: parse_ipv4;
#ifdef WITH_IPV6
            ETHERTYPE_IPV6: parse_ipv6;
#endif // WITH_IPV6
            default: accept;
        }
    }

    state parse_vlan_tag {
        packet.extract(hdr.vlan_tag);
        transition select(hdr.vlan_tag.ether_type){
            ETHERTYPE_ARP: parse_arp;
            ETHERTYPE_IPV4: parse_ipv4;
#ifdef WITH_IPV6
            ETHERTYPE_IPV6: parse_ipv6;
#endif // WITH_IPV6
            ETHERTYPE_MPLS: parse_mpls;
            default: accept;
        }
    }

    state parse_mpls {
        packet.extract(hdr.mpls);
        // There is only one MPLS label for this fabric.
        // Assume header after MPLS header is IP/IPv6
        // Lookup first 4 bits for version
        transition select(packet.lookahead<bit<IP_VER_LENGTH>>()) {
            //The packet should be either IPv4 or IPv6.
            IP_VERSION_4: parse_ipv4;
#ifdef WITH_IPV6
            IP_VERSION_6: parse_ipv6;
#endif // WITH_IPV6
            default: parse_ethernet;
        }
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        fabric_metadata.ip_proto = hdr.ipv4.protocol;
        //Need header verification?
        transition select(hdr.ipv4.protocol) {
            PROTO_TCP: parse_tcp;
            PROTO_UDP: parse_udp;
            PROTO_ICMP: parse_icmp;
            default: accept;
        }
    }

#ifdef WITH_IPV6
    state parse_ipv6 {
        packet.extract(hdr.ipv6);
        fabric_metadata.ip_proto = hdr.ipv6.next_hdr;
        transition select(hdr.ipv6.next_hdr) {
            PROTO_TCP: parse_tcp;
            PROTO_UDP: parse_udp;
            PROTO_ICMPV6: parse_icmp;
            default: accept;
        }
    }
#endif // WITH_IPV6

    state parse_arp {
        packet.extract(hdr.arp);
        transition accept;
    }

    state parse_tcp {
        packet.extract(hdr.tcp);
        fabric_metadata.l4_src_port = hdr.tcp.src_port;
        fabric_metadata.l4_dst_port = hdr.tcp.dst_port;
#ifdef WITH_INT
        transition select(hdr.ipv4.isValid() && ((hdr.ipv4.dscp & INT_DSCP) == INT_DSCP)) {
            true: parse_intl4_shim;
            default: accept;
        }
#else
        transition accept;
#endif // WITH_INT
    }

    state parse_udp {
        packet.extract(hdr.udp);
        fabric_metadata.l4_src_port = hdr.udp.src_port;
        fabric_metadata.l4_dst_port = hdr.udp.dst_port;
#ifdef WITH_SPGW
        transition select(hdr.udp.dst_port) {
            UDP_PORT_GTPU: parse_gtpu;
            default: accept;
        }
#elif WITH_INT
        transition select(hdr.ipv4.isValid() && (hdr.ipv4.dscp & INT_DSCP) == INT_DSCP) {
            true: parse_intl4_shim;
            default: accept;
        }
#else
        transition accept;
#endif // WITH_SPGW, WITH_INT
    }

    state parse_icmp {
        packet.extract(hdr.icmp);
        transition accept;
    }

#ifdef WITH_INT
    state parse_intl4_shim {
        packet.extract(hdr.intl4_shim);
        transition parse_int_header;
    }

    state parse_int_header {
        packet.extract(hdr.int_header);
        // If there is no INT metadata but the INT header (and corresponding shim header
        // and tail header) exists, default value of length field in shim header
        // should be INT_HEADER_LEN_WORD.
        fabric_metadata.int_meta.metadata_len = hdr.intl4_shim.len - INT_HEADER_LEN_WORD;
        transition select (fabric_metadata.int_meta.metadata_len) {
            0: parse_intl4_tail;
            default: parse_int_data;
        }
    }

    state parse_int_data {
        // Parse INT metadata, not INT header, INT shim header and INT tail header
        packet.extract(hdr.int_data, (bit<32>) ((hdr.intl4_shim.len - INT_HEADER_LEN_WORD) << 5));
        transition parse_intl4_tail;
    }

    state parse_intl4_tail {
        packet.extract(hdr.intl4_tail);
        transition accept;
    }
#endif // WITH_INT

#ifdef WITH_SPGW
    state parse_gtpu {
        transition select(hdr.ipv4.dst_addr[31:32-S1U_SGW_PREFIX_LEN]) {
            // Avoid parsing GTP and inner headers if we know this GTP packet
            // is not to be processed by this switch.
            // FIXME: use parser value sets when support is ready in ONOS.
            // To set the S1U_SGW_PREFIX value at runtime.
            S1U_SGW_PREFIX[31:32-S1U_SGW_PREFIX_LEN]: do_parse_gtpu;
            default: accept;
        }
    }

    state do_parse_gtpu {
        packet.extract(hdr.gtpu);
        transition parse_inner_ipv4;
    }

    state parse_inner_ipv4 {
        packet.extract(hdr.inner_ipv4);
        transition select(hdr.inner_ipv4.protocol) {
            PROTO_TCP: parse_tcp;
            PROTO_UDP: parse_inner_udp;
            PROTO_ICMP: parse_icmp;
            default: accept;
        }
    }

    state parse_inner_udp {
        packet.extract(hdr.inner_udp);
        fabric_metadata.l4_src_port = hdr.inner_udp.src_port;
        fabric_metadata.l4_dst_port = hdr.inner_udp.dst_port;
#ifdef WITH_INT
        transition select(hdr.ipv4.isValid() && (hdr.ipv4.dscp & INT_DSCP) == INT_DSCP) {
            true: parse_intl4_shim;
            default: accept;
        }
#else
        transition accept;
#endif // WITH_INT
    }
#endif // WITH_SPGW
}

control FabricDeparser(packet_out packet, in parsed_headers_t hdr) {
    apply {
        packet.emit(hdr.packet_in);
#ifdef WITH_INT
        packet.emit(hdr.report_ethernet);
        packet.emit(hdr.report_ipv4);
        packet.emit(hdr.report_udp);
        packet.emit(hdr.report_fixed_header);
#endif // WITH_INT
        packet.emit(hdr.ethernet);
        packet.emit(hdr.vlan_tag);
        packet.emit(hdr.mpls);
        packet.emit(hdr.arp);
#ifdef WITH_SPGW
        packet.emit(hdr.gtpu_ipv4);
        packet.emit(hdr.gtpu_udp);
        packet.emit(hdr.gtpu);
#endif // WITH_SPGW
        packet.emit(hdr.ipv4);
#ifdef WITH_IPV6
        packet.emit(hdr.ipv6);
#endif // WITH_IPV6
        packet.emit(hdr.tcp);
        packet.emit(hdr.udp);
        packet.emit(hdr.icmp);
#ifdef WITH_INT
        packet.emit(hdr.intl4_shim);
        packet.emit(hdr.int_header);
        packet.emit(hdr.int_switch_id);
        packet.emit(hdr.int_port_ids);
        packet.emit(hdr.int_hop_latency);
        packet.emit(hdr.int_q_occupancy);
        packet.emit(hdr.int_ingress_tstamp);
        packet.emit(hdr.int_egress_tstamp);
        packet.emit(hdr.int_q_congestion);
        packet.emit(hdr.int_egress_tx_util);
        packet.emit(hdr.int_data);
        packet.emit(hdr.intl4_tail);
#endif // WITH_INT
    }
}

#endif
