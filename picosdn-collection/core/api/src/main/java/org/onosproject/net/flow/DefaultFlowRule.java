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
package org.onosproject.net.flow;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.TableId.Type.INDEX;

import java.util.Objects;
import java.util.UUID;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;

import com.google.common.hash.Funnel;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Default flow rule.
 */
public class DefaultFlowRule implements FlowRule {

    private final DeviceId deviceId;
    private final int priority;
    private final TrafficSelector selector;
    private final TrafficTreatment treatment;
    private final long created;

    private final FlowId id;

    private final Short appId;

    private final int timeout;
    private final boolean permanent;
    private final int hardTimeout;
    private final FlowRemoveReason reason;
    private final GroupId groupId;

    private final TableId tableId;
    private final FlowRuleExtPayLoad payLoad;
    private UUID entityUuid;

    /**
     * Creates a new flow rule from an existing rule.
     *
     * @param rule new flow rule
     */
    public DefaultFlowRule(FlowRule rule) {
        this.deviceId = rule.deviceId();
        this.priority = rule.priority();
        this.selector = rule.selector();
        this.treatment = rule.treatment();
        this.appId = rule.appId();
        this.groupId = rule.groupId();
        this.id = rule.id();
        this.timeout = rule.timeout();
        this.hardTimeout = rule.hardTimeout();
        this.reason = rule.reason();
        this.permanent = rule.isPermanent();
        this.created = System.currentTimeMillis();
        this.tableId = rule.table();
        this.payLoad = rule.payLoad();
    }

    private DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, Integer priority, FlowId flowId,
            Boolean permanent, Integer timeout, Integer hardTimeout,
            FlowRemoveReason reason, TableId tableId) {

        this.deviceId = deviceId;
        this.selector = selector;
        this.treatment = treatment;
        this.priority = priority;
        this.appId = (short) (flowId.value() >>> 48);
        this.id = flowId;
        this.permanent = permanent;
        this.timeout = timeout;
        this.hardTimeout = hardTimeout;
        this.reason = reason;
        this.tableId = tableId;
        this.created = System.currentTimeMillis();

        // FIXME: fields below will be removed.
        this.groupId = new GroupId(0);
        this.payLoad = null;
    }

    /**
     * Support for the third party flow rule. Creates a flow rule of flow table.
     *
     * @param deviceId  the identity of the device where this rule applies
     * @param selector  the traffic selector that identifies what traffic this
     *                  rule
     * @param treatment the traffic treatment that applies to selected traffic
     * @param priority  the flow rule priority given in natural order
     * @param appId     the application id of this flow
     * @param timeout   the timeout for this flow requested by an application
     * @param permanent whether the flow is permanent i.e. does not time out
     * @param payLoad   3rd-party origin private flow
     * @deprecated in Junco release. Use FlowRule.Builder instead.
     */
    @Deprecated
    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority, ApplicationId appId,
            int timeout, boolean permanent, FlowRuleExtPayLoad payLoad) {
        this(deviceId, selector, treatment, priority, appId, timeout, 0,
                permanent, payLoad);
    }

    /**
     * Support for the third party flow rule. Creates a flow rule of flow table.
     *
     * @param deviceId    the identity of the device where this rule applies
     * @param selector    the traffic selector that identifies what traffic this
     *                    rule
     * @param treatment   the traffic treatment that applies to selected traffic
     * @param priority    the flow rule priority given in natural order
     * @param appId       the application id of this flow
     * @param timeout     the timeout for this flow requested by an application
     * @param hardTimeout the hard timeout located switch's flow table for this
     *                    flow requested by an application
     * @param permanent   whether the flow is permanent i.e. does not time out
     * @param payLoad     3rd-party origin private flow
     * @deprecated in Junco release. Use FlowRule.Builder instead.
     */
    @Deprecated
    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority, ApplicationId appId,
            int timeout, int hardTimeout, boolean permanent,
            FlowRuleExtPayLoad payLoad) {

        checkArgument(priority >= MIN_PRIORITY,
                "Priority cannot be less than " + MIN_PRIORITY);
        checkArgument(priority <= MAX_PRIORITY,
                "Priority cannot be greater than " + MAX_PRIORITY);

        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.appId = appId.id();
        this.groupId = new GroupId(0);
        this.timeout = timeout;
        this.reason = FlowRemoveReason.NO_REASON;
        this.hardTimeout = hardTimeout;
        this.permanent = permanent;
        this.tableId = DEFAULT_TABLE;
        this.created = System.currentTimeMillis();
        this.payLoad = payLoad;

        /*
         * id consists of the following. | appId (16 bits) | groupId (16 bits) |
         * flowId (32 bits) |
         */
        this.id = FlowId.valueOf((long) this.appId << 48
                | (long) this.groupId.id() << 32 | this.hash() & 0xffffffffL);
    }

    /**
     * Support for the third party flow rule. Creates a flow rule of group
     * table.
     *
     * @param deviceId  the identity of the device where this rule applies
     * @param selector  the traffic selector that identifies what traffic this
     *                  rule
     * @param treatment the traffic treatment that applies to selected traffic
     * @param priority  the flow rule priority given in natural order
     * @param appId     the application id of this flow
     * @param groupId   the group id of this flow
     * @param timeout   the timeout for this flow requested by an application
     * @param permanent whether the flow is permanent i.e. does not time out
     * @param payLoad   3rd-party origin private flow
     * @deprecated in Junco release. Use FlowRule.Builder instead.
     */
    @Deprecated
    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority, ApplicationId appId,
            GroupId groupId, int timeout, boolean permanent,
            FlowRuleExtPayLoad payLoad) {
        this(deviceId, selector, treatment, priority, appId, groupId, timeout,
                0, permanent, payLoad);
    }

    /**
     * Support for the third party flow rule. Creates a flow rule of group
     * table.
     *
     * @param deviceId    the identity of the device where this rule applies
     * @param selector    the traffic selector that identifies what traffic this
     *                    rule
     * @param treatment   the traffic treatment that applies to selected traffic
     * @param priority    the flow rule priority given in natural order
     * @param appId       the application id of this flow
     * @param groupId     the group id of this flow
     * @param timeout     the timeout for this flow requested by an application
     * @param hardTimeout the hard timeout located switch's flow table for this
     *                    flow requested by an application
     * @param permanent   whether the flow is permanent i.e. does not time out
     * @param payLoad     3rd-party origin private flow
     * @deprecated in Junco release. Use FlowRule.Builder instead.
     */
    @Deprecated
    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority, ApplicationId appId,
            GroupId groupId, int timeout, int hardTimeout, boolean permanent,
            FlowRuleExtPayLoad payLoad) {

        checkArgument(priority >= MIN_PRIORITY,
                "Priority cannot be less than " + MIN_PRIORITY);
        checkArgument(priority <= MAX_PRIORITY,
                "Priority cannot be greater than " + MAX_PRIORITY);

        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.appId = appId.id();
        this.groupId = groupId;
        this.timeout = timeout;
        this.reason = FlowRemoveReason.NO_REASON;
        this.hardTimeout = hardTimeout;
        this.permanent = permanent;
        this.created = System.currentTimeMillis();
        this.tableId = DEFAULT_TABLE;
        this.payLoad = payLoad;

        /*
         * id consists of the following. | appId (16 bits) | groupId (16 bits) |
         * flowId (32 bits) |
         */
        this.id = FlowId.valueOf((long) this.appId << 48
                | (long) this.groupId.id() << 32 | this.hash() & 0xffffffffL);
    }

    @Override
    public FlowId id() {
        return id;
    }

    @Override
    public short appId() {
        return appId;
    }

    @Override
    public GroupId groupId() {
        return groupId;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public TrafficSelector selector() {
        return selector;
    }

    @Override
    public TrafficTreatment treatment() {
        return treatment;
    }

    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public int hashCode() {
        return Objects.hash(deviceId, selector, tableId, payLoad);
    }

    // FIXME do we need this method in addition to hashCode()?
    private int hash() {
        return Objects.hash(deviceId, selector, tableId, payLoad);
    }

    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultFlowRule) {
            DefaultFlowRule that = (DefaultFlowRule) obj;
            return Objects.equals(deviceId, that.deviceId)
                    && Objects.equals(priority, that.priority)
                    && Objects.equals(selector, that.selector)
                    && Objects.equals(tableId, that.tableId)
                    && Objects.equals(payLoad, that.payLoad);
        }
        return false;
    }

    @Override
    public boolean exactMatch(FlowRule rule) {
        return this.equals(rule) && Objects.equals(this.id, rule.id())
                && Objects.equals(this.treatment, rule.treatment());
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", Long.toHexString(id.value()))
                .add("deviceId", deviceId).add("priority", priority)
                .add("selector", selector.criteria())
                .add("treatment", treatment == null ? "N/A" : treatment)
                .add("tableId", tableId).add("created", created)
                .add("payLoad", payLoad).toString();
    }

    @Override
    public int timeout() {
        return timeout;
    }

    @Override
    public int hardTimeout() {
        return hardTimeout;
    }

    @Override
    public FlowRemoveReason reason() {
        return reason;
    }

    @Override
    public boolean isPermanent() {
        return permanent;
    }

    @Override
    public int tableId() {
        // Workaround until we remove this method. Deprecated in Loon.
        return tableId.type() == INDEX ? ((IndexTableId) tableId).id()
                : tableId.hashCode();
    }

    @Override
    public TableId table() {
        return tableId;
    }

    /**
     * Returns the wallclock time that the flow was created.
     *
     * @return creation time in milliseconds since epoch
     */
    public long created() {
        return created;
    }

    /**
     * Returns a default flow rule builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default flow rule builder.
     */
    public static final class Builder implements FlowRule.Builder {

        private FlowId flowId;
        private ApplicationId appId;
        private Integer priority;
        private DeviceId deviceId;
        private TableId tableId = DEFAULT_TABLE;
        private TrafficSelector selector = DefaultTrafficSelector.builder()
                .build();
        private TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .build();
        private Integer timeout;
        private Boolean permanent;
        private Integer hardTimeout = 0;
        private FlowRemoveReason reason = FlowRemoveReason.NO_REASON;

        @Override
        public FlowRule.Builder withCookie(long cookie) {
            this.flowId = FlowId.valueOf(cookie);
            return this;
        }

        @Override
        public FlowRule.Builder fromApp(ApplicationId appId) {
            this.appId = appId;
            return this;
        }

        @Override
        public FlowRule.Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public FlowRule.Builder forDevice(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public FlowRule.Builder forTable(int tableId) {
            this.tableId = IndexTableId.of(tableId);
            return this;
        }

        @Override
        public FlowRule.Builder forTable(TableId tableId) {
            this.tableId = tableId;
            return this;
        }

        @Override
        public FlowRule.Builder withSelector(TrafficSelector selector) {
            this.selector = selector;
            return this;
        }

        @Override
        public FlowRule.Builder withTreatment(TrafficTreatment treatment) {
            this.treatment = checkNotNull(treatment);
            return this;
        }

        @Override
        public FlowRule.Builder makePermanent() {
            this.timeout = 0;
            this.permanent = true;
            return this;
        }

        @Override
        public FlowRule.Builder makeTemporary(int timeout) {
            this.permanent = false;
            this.timeout = timeout;
            return this;
        }

        @Override
        public FlowRule.Builder withHardTimeout(int timeout) {
            this.permanent = false;
            this.hardTimeout = timeout;
            this.timeout = timeout;
            return this;
        }

        @Override
        public FlowRule.Builder withReason(FlowRemoveReason reason) {
            this.reason = reason;
            return this;
        }

        @Override
        public FlowRule build() {
            FlowId localFlowId;
            checkNotNull(tableId, "Table id cannot be null");
            checkArgument(flowId != null ^ appId != null,
                    "Either an application"
                            + " id or a cookie must be supplied");
            checkNotNull(selector, "Traffic selector cannot be null");
            checkArgument(timeout != null || permanent != null,
                    "Must either have " + "a timeout or be permanent");
            checkNotNull(deviceId, "Must refer to a device");
            checkNotNull(priority, "Priority cannot be null");
            checkArgument(priority >= MIN_PRIORITY,
                    "Priority cannot be less than " + MIN_PRIORITY);
            checkArgument(priority <= MAX_PRIORITY,
                    "Priority cannot be greater than " + MAX_PRIORITY);
            // Computing a flow ID based on appId takes precedence over setting
            // the flow ID directly
            if (appId != null) {
                localFlowId = computeFlowId(appId);
            } else {
                localFlowId = flowId;
            }

            return new DefaultFlowRule(deviceId, selector, treatment, priority,
                    localFlowId, permanent, timeout, hardTimeout, reason,
                    tableId);
        }

        private FlowId computeFlowId(ApplicationId appId) {
            return FlowId
                    .valueOf((long) appId.id() << 48 | hash() & 0xffffffffL);
        }

        private int hash() {
            // Guava documentation recommends using putUnencodedChars to hash
            // raw character bytes within any encoding
            // unless cross-language compatibility is needed. See the
            // Hasher.putString documentation for more info.
            Funnel<TrafficSelector> selectorFunnel = (from, into) -> from
                    .criteria()
                    .forEach(c -> into.putUnencodedChars(c.toString()));

            HashFunction hashFunction = Hashing.murmur3_32();
            HashCode hashCode = hashFunction.newHasher()
                    .putUnencodedChars(deviceId.toString())
                    .putObject(selector, selectorFunnel).putInt(priority)
                    .putUnencodedChars(tableId.toString()).hash();

            return hashCode.asInt();
        }
    }

    @Override
    public FlowRuleExtPayLoad payLoad() {
        return payLoad;
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
