package edu.mit.ll.provsdn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.Event;
import org.onosproject.net.Host;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.security.ProvActivity;
import org.onosproject.security.ProvApiCallType;
import org.onosproject.security.ProvEntity;
import org.onosproject.security.ProvHook;
import org.onosproject.security.ProvService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProvSDN provenance manager.
 *
 * Collects provenance information from ProvHook, maintains some state about the
 * control plane, and generates serialized PROV output suitable for analysis
 *
 * @author Benjamin Ujcich <benjamin.ujcich@ll.mit.edu> <ujcich2@illinois.edu>
 * @author Samuel Jero <samuel.jero@ll.mit.edu>
 * @version 2.0
 */
@Component(immediate = true)
@Service
public class ProvManager implements ProvService {

    /*
     * Internal state
     */

    // ProvManager's view of active control plane state objects
    private Map<ProvEntity, W3CProvEntity> provEntityToW3CProvEntity = Collections
            .synchronizedMap(new HashMap<>());

    // Current instance of a listener
    private Map<String, W3CProvActivity> activeListeners = Collections
            .synchronizedMap(new HashMap<>());

    // Map of OpenFlow identifiers (cookies) to associated flow rules
    private Map<Long, W3CProvEntity> cookieToFlowRule = Collections
            .synchronizedMap(new HashMap<>());

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Configuration
     */
    private static final boolean IGNORE_STATS = true;
    private static final boolean IGNORE_RESOURCES = true;

    /**
     * Activate provenance collection.
     */
    @Activate
    protected void activate() {
        /* Start provenance hook */
        Timer tmr = new Timer();
        TimerTask action = new TimerTask() {
            @Override
            public void run() {
                ProvHook.start();
                log.info("Enabled provenance hooks");
            }
        };
        tmr.schedule(action, 500);
        log.info("Started");
    }

    /**
     * Deactivate provenance collection.
     */
    @Deactivate
    protected void deactivate() {
        ProvHook.stop();
        log.info("Disabled provenance hooks");
        log.info("Stopped");
    }

    /**
     * Record an activity being dispatched.
     *
     * This should be called within the listener registry or the packet
     * processor before the event or packet is dispatched, respectively.
     *
     * Even if we don't have any listeners, we should still record this in case
     * the related effect (e.g., new control plane object, packet out) has some
     * consequences later on.
     *
     * @param activity
     */
    @Override
    synchronized public void recordDispatch(ProvActivity activity) {

        /* skip if we don't have this information already */
        if (activity == null) {
            log.warn("recordDispatch: Activity is null; skipping.");
            return;
        }

        /* initialize */
        activity.setActivityUuid(UUID.randomUUID());

        if (activity instanceof Event) {
            Event event = (Event) activity;

            /*
             * Special case of network identifier evolution: if host object is
             * modified, then link old and new host objects via revision
             * relation
             */
            if (event instanceof HostEvent) {
                HostEvent hostEvent = (HostEvent) event;
                Host oldHost = hostEvent.prevSubject();
                Host newHost = hostEvent.subject();
                if (oldHost != null && newHost != null) {
                    W3CProvEntity w3cEntityOldHost = getOrInitializeEntity(
                            oldHost);
                    W3CProvEntity w3cEntityNewHost = getOrInitializeEntity(
                            newHost);
                    W3CProvRelation wasRevisionOf = new W3CProvRelation(
                            w3cEntityNewHost, w3cEntityOldHost,
                            W3CProvRelationType.WAS_REVISION_OF);
                    writeOut(wasRevisionOf.toJson());
                }
            }

        } else if (activity instanceof PacketContext) {

            /*
             * Special case of data plane model: associate an inbound packet
             * with the flow rule that requested it
             */
            PacketContext context = (PacketContext) activity;
            if (context.inPacket() != null
                    && context.inPacket() instanceof ProvEntity) {
                InboundPacket inPacket = context.inPacket();

                Long cookie = inPacket.cookie().get();
                if (cookieToFlowRule.containsKey(cookie)) {
                    W3CProvEntity w3cEntityPacket = getOrInitializeEntity(
                            inPacket);
                    W3CProvEntity w3cEntityFlowRule = cookieToFlowRule
                            .get(cookie);
                    W3CProvRelation wasDerivedFrom = new W3CProvRelation(
                            w3cEntityPacket, w3cEntityFlowRule,
                            W3CProvRelationType.WAS_DERIVED_FROM);
                    writeOut(wasDerivedFrom.toJson());
                }

            }

        }

    }

    /**
     * Record an activity being listened to.
     *
     * This should be called at the start of when an event listener or packet
     * processor listens for an event or processes a packet, respectively.
     * During the listening or processing (up until the point of the next
     * listener or processor), link all API calls to this activity.
     *
     * @param activity
     * @param listener
     */
    @Override
    synchronized public void recordListen(ProvActivity activity,
            Class listener) {

        /* skip if we don't have this information already */
        if (activity == null || listener == null) {
            log.warn("recordListen: Activity or listener is null; skipping.");
            return;
        } else if (activity.getActivityUuid() == null) {
            log.warn("recordListen: Activity has no UUID; skipping.");
            return;
        }

        /*
         * stats-related events are numerous and may not always affect the
         * control plane, so skip these as needed
         */
        if (IGNORE_STATS && isStatsRelated(activity, listener)) {
            return;
        }

        /*
         * resource-related events are numerous and not particularly
         * descriptive, so skip these as needed
         */
        if (IGNORE_RESOURCES && isResourceRelated(activity, listener)) {
            return;
        }

        /* create W3CProvActivity for this listener invocation (done once) */
        W3CProvActivity w3cActivity = new W3CProvActivity(UUID.randomUUID(),
                activity, listener.getName(), "");
        writeOut(w3cActivity.toJson());
        updateActiveListener(listener, w3cActivity);

        /* W3CProvActivity 'used' the entity related to that dispatch */
        if (activity instanceof Event) {
            Event event = (Event) activity;
            if (event.subject() != null
                    && event.subject() instanceof ProvEntity) {
                ProvEntity entity = (ProvEntity) event.subject();
                W3CProvEntity w3cEntity = getOrInitializeEntity(entity);
                W3CProvRelation used = new W3CProvRelation(w3cActivity,
                        w3cEntity, W3CProvRelationType.USED);
                writeOut(used.toJson());
            }
        } else if (activity instanceof PacketContext) {
            PacketContext context = (PacketContext) activity;
            if (context.inPacket() != null
                    && context.inPacket() instanceof ProvEntity) {
                ProvEntity entity = context.inPacket();
                W3CProvEntity w3cEntity = getOrInitializeEntity(entity);
                W3CProvRelation used = new W3CProvRelation(w3cActivity,
                        w3cEntity, W3CProvRelationType.USED);
                writeOut(used.toJson());
            }
        }

    }

    /**
     * Record an API call.
     *
     * Based on the call stack, we can infer which listener this belonged to so
     * that we can build the provenance graph appropriately.
     *
     * @param type              API call type
     * @param entity            entity related to this API call (can be null)
     * @param afterUpdateEntity (optional) updated entity related to this API
     *                          call, otherwise null
     * @param location          call stack
     */
    @Override
    public void recordApiCall(ProvApiCallType type, ProvEntity entity,
            ProvEntity afterUpdateEntity, Throwable location) {

        /*
         * find the most recent listener in the call stack and if it has an
         * associated W3CProvActivity, link it accordingly
         */
        StackTraceElement[] stackTrace = location.getStackTrace();
        W3CProvActivity w3cActivity = null;
        for (StackTraceElement element : stackTrace) {
            if (activeListeners.containsKey(element.getClassName())) {
                w3cActivity = activeListeners.get(element.getClassName());
                break;
            }

        }

        /*
         * Build appropriate relations (and perhaps new entities).
         *
         * Synchronize this to avoid read/write concurrency issues.
         */
        synchronized (this) {

            switch (type) {
            case CREATE:
                handleCreate(entity, w3cActivity);
                break;
            case READ:
                handleRead(entity, w3cActivity);
                break;
            case UPDATE:
                handleUpdate(entity, afterUpdateEntity, w3cActivity);
                break;
            case DELETE:
                handleDelete(entity, w3cActivity);
                break;
            default:
                break;
            }
        }

    }

    /**
     * Record a derivation from an entity to another entity (WAS_DERIVED_FROM).
     *
     * Use this if: 1) we can't infer from the API call / call stack about the
     * relation between using some entity to generate another entity, or 2) we
     * want to link a larger entity to its details (e.g., a Host object linked
     * to its HostDescription object).
     *
     * Use WAS_REVISION_OF for entity updates.
     *
     * @param child
     * @param parent
     */
    @Override
    public void recordDerivation(ProvEntity child, ProvEntity parent) {

        if (child != null && parent != null) {
            W3CProvEntity w3cEntityChild = getOrInitializeEntity(child);
            W3CProvEntity w3cEntityParent = getOrInitializeEntity(parent);
            W3CProvRelation wasDerivedFrom = new W3CProvRelation(w3cEntityChild,
                    w3cEntityParent, W3CProvRelationType.WAS_DERIVED_FROM);
            writeOut(wasDerivedFrom.toJson());
        } else {
            log.warn("recordDerivation: Child or parent is null; skipping.");
        }

    }

    /**
     * Get or initialize ProvEntity.
     *
     * Initializes ProvEntity object's UUID if it does not already contain one.
     * Generates W3CProvEntity object and PROV-N serialization if this has not
     * already happened. Returns respective W3CProvEntity object.
     *
     * Since this sets UUIDs and stores a mapping, this method should be
     * synchronized to avoid re-initialization.
     *
     * @param entity
     * @return a W3CProvEntity
     */
    synchronized private W3CProvEntity getOrInitializeEntity(
            ProvEntity entity) {

        /*
         * if we have ProvEntity already, then this means we don't need to
         * regenerate anything (or output it again)
         */
        if (provEntityToW3CProvEntity.containsKey(entity)) {
            return provEntityToW3CProvEntity.get(entity);
        } else {
            /* set UUID if not already set */
            if (entity.getEntityUuid() == null) {
                entity.setEntityUuid(UUID.randomUUID());
            }

            /* set up W3CProvEntity object */
            W3CProvEntity w3cEntity = new W3CProvEntity(entity.getEntityUuid(),
                    entity, "", entity.toString());
            provEntityToW3CProvEntity.put(entity, w3cEntity);
            /* only time we should write out this W3CProvEntity */
            writeOut(w3cEntity.toJson());

            /* if flow rule, add to cookie map (used by packets later) */
            if (entity instanceof FlowRule) {
                FlowRule flowRule = (FlowRule) entity;
                cookieToFlowRule.put(flowRule.id().id(), w3cEntity);
            }

            return w3cEntity;

        }

    }

    /**
     * Determine if activity relates to resource events.
     *
     * @param activity
     * @param listener
     * @return
     */
    private boolean isResourceRelated(ProvActivity activity, Class listener) {
        if (activity instanceof Event) {
            Event event = (Event) activity;
            if (event.type().toString().startsWith("RESOURCE")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if activity relates to statistics.
     *
     * @param activity
     * @param listener
     * @return
     */
    private boolean isStatsRelated(ProvActivity activity, Class listener) {
        if (listener.getName().contains("Statistic")) {
            return true;
        }
        return false;
    }

    /**
     * Remove an active listener (if one exists) and add a new active listener.
     *
     * After this method is called, any subsequent API calls will be associated
     * with the new W3CProvActivity. Thus, recordListen() sets activeListeners
     * and recordApiCall() uses activeListeners.
     *
     * Since the listener check and listener removal steps are not atomic, this
     * method should be synchronized.
     *
     * @param listener
     * @param w3cActivity
     */
    synchronized private void updateActiveListener(Class listener,
            W3CProvActivity w3cActivity) {
        /* remove previous W3CProvActivity reference if it exists */
        if (activeListeners.containsKey(listener.getName())) {
            activeListeners.remove(listener.getName());
        }
        activeListeners.put(listener.getName(), w3cActivity);
    }

    /**
     * Handle relation generation for an entity being deleted.
     * 
     * Functionally equivalent to a read, except that this represents
     * invalidation of data.
     *
     * @param entity
     * @param w3cActivity
     */
    private void handleDelete(ProvEntity entity, W3CProvActivity w3cActivity) {

        /* Don't handle deletions for now because of causal reasoning */

    }

    /**
     * Handle relation generation for an entity being updated.
     * 
     * Depending on the information we get, treat updates as: 1) deletes (if the
     * new entity is null); 2) creations (if we start from null); 3) creations
     * (if both before and after entities have the same data); 4) revisions
     * (between old and new) + generation (of new) + invalidation (of old)
     *
     * @param entity
     * @param afterUpdateEntity
     * @param w3cActivity
     */
    private void handleUpdate(ProvEntity entity, ProvEntity afterUpdateEntity,
            W3CProvActivity w3cActivity) {

        /*
         * if entity is null but afterUpdateEntity isn't, then that means this
         * was probably a creation, so handle it that way
         */
        if (entity == null && afterUpdateEntity != null) {
            log.warn(
                    "recordApiCall(handleUpdate): Entity is null; treating as a creation.");
            handleCreate(afterUpdateEntity, w3cActivity);
            return;
        }
        /*
         * if afterUpdateEntity is null but entity wasn't, then this is probably
         * a deletion
         */
        else if (entity != null && afterUpdateEntity == null) {
            log.warn(
                    "recordApiCall(handleUpdate): Updated entity is null; treating as a deletion.");
            handleDelete(entity, w3cActivity);
            return;
        }
        /*
         * if both are null, we can't do anything
         */
        else if (entity == null && afterUpdateEntity == null) {
            log.warn(
                    "recordApiCall(handleUpdate): Entity and updated entity are null; skipping.");
            return;
        }
        /*
         * otherwise, this is an update assume that string representation of
         * entities uniquely identifies them
         */

        if (entity.toString().equals(afterUpdateEntity)) {
            /*
             * no update actually happened, so treat this as a creation (of the
             * updated activity)
             */
            log.warn(
                    "recordApiCall(handleUpdate): Entity and updated entity are same value; treating as a creation.");
            handleCreate(afterUpdateEntity, w3cActivity);
        } else {
            /*
             * update happened, so treat this as a revision (+ generation and
             * invalidation)
             */

            W3CProvEntity w3cEntityOld = getOrInitializeEntity(entity);
            W3CProvEntity w3cEntityNew = getOrInitializeEntity(
                    afterUpdateEntity);

            /* revision: new entity was revision of old entity */
            W3CProvRelation wasRevisionOf = new W3CProvRelation(w3cEntityNew,
                    w3cEntityOld, W3CProvRelationType.WAS_REVISION_OF);
            writeOut(wasRevisionOf.toJson());

            /*
             * if we can find a related activity, we can also add generation and
             * invalidation relations
             */
            if (w3cActivity == null) {
                log.warn(
                        "handleUpdate: Could not find activity related to ProvEntity {}; skipping relations.",
                        afterUpdateEntity.toString());
                return;
            }
            /* generation: new entity was generated by activity */
            W3CProvRelation wasGeneratedBy = new W3CProvRelation(w3cEntityNew,
                    w3cActivity, W3CProvRelationType.WAS_GENERATED_BY);
            writeOut(wasGeneratedBy.toJson());

        }

        return;

    }

    /**
     * Handle relation generation for an entity being read.
     *
     * Read semantics: Link the activity to the entity.
     *
     * @param entity
     * @param w3cActivity
     */
    private void handleRead(ProvEntity entity, W3CProvActivity w3cActivity) {

        if (entity == null) {
            log.warn("recordApiCall(handleRead): Entity is null; skipping.");
            return;
        }
        W3CProvEntity w3cEntity = getOrInitializeEntity(entity);
        if (w3cActivity == null) {
            log.warn(
                    "handleRead: Could not find activity related to ProvEntity {}; skipping relation.",
                    entity.toString());
            return;
        }
        W3CProvRelation used = new W3CProvRelation(w3cActivity, w3cEntity,
                W3CProvRelationType.USED);
        writeOut(used.toJson());

    }

    /**
     * Handle relation generation for an entity being created (written).
     *
     * @param entity
     * @param w3cActivity
     */
    private void handleCreate(ProvEntity entity, W3CProvActivity w3cActivity) {

        if (entity == null) {
            log.warn("recordApiCall(handleCreate): Entity is null; skipping.");
            return;
        }
        W3CProvEntity w3cEntity = getOrInitializeEntity(entity);
        if (w3cActivity == null) {
            log.warn(
                    "handleCreate: Could not find activity related to ProvEntity {}; skipping relation.",
                    entity.toString());
            return;
        }
        W3CProvRelation wasGeneratedBy = new W3CProvRelation(w3cEntity,
                w3cActivity, W3CProvRelationType.WAS_GENERATED_BY);
        writeOut(wasGeneratedBy.toJson());

    }

    /**
     * Write out provenance to output.
     *
     * @param strOut
     */
    private void writeOut(String strOut) {
        // FIXME: write to file; for now, write to debug
        log.debug("{}", strOut);
    }

}
