package edu.mit.ll.provsdn;

import java.util.UUID;

import org.onosproject.security.ProvEntity;

/**
 * W3C PROV Entity class representation.
 *
 * W3CProvEntity and ProvEntity have a one-to-one relationship.
 *
 * @author Benjamin Ujcich <benjamin.ujcich@ll.mit.edu> <ujcich2@illinois.edu>
 * @author Samuel Jero <samuel.jero@ll.mit.edu>
 * @version 2.0
 */
public class W3CProvEntity implements W3CProvObject {

    private UUID uuid;
    private ProvEntity entity;
    private String name;
    private String value;
    private long ts;

    public W3CProvEntity(UUID uuid, ProvEntity entity, String name,
            String value) {
        super();
        this.uuid = uuid;
        this.entity = entity;
        this.name = name;
        this.value = value;
        this.ts = System.currentTimeMillis();
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ProvEntity getEntity() {
        return entity;
    }

    public void setEntity(ProvEntity entity) {
        this.entity = entity;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toProvN() {
        return "entity(" + uuid.toString() + ", [name=\"" + name
                + "\", value=\"" + value + "\", ts=\"" + String.valueOf(ts)
                + "\"])";
    }

    @Override
    public String toJson() {
        return "{\"w3cProvType\":\"entity\",\"uuid\":\"" + uuid.toString()
                + "\",\"name\":\"" + name + "\",\"value\":\"" + value
                + "\",\"ts\":\"" + String.valueOf(ts) + "\"}";
    }

}
