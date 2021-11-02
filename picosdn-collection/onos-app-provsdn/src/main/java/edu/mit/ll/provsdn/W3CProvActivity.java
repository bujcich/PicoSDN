package edu.mit.ll.provsdn;

import java.util.UUID;

import org.onosproject.security.ProvActivity;

/**
 * W3C PROV Activity class representation.
 *
 * Note that one ProvActivity instance may correspond to one or more
 * W3CProvActivity instances. As an example, the event dispatch "process" is
 * considered as one ProvActivity, while each listener of that event (i.e., each
 * loop) is a W3CProvActivity.
 *
 * @author Benjamin Ujcich <benjamin.ujcich@ll.mit.edu> <ujcich2@illinois.edu>
 * @author Samuel Jero <samuel.jero@ll.mit.edu>
 * @version 2.0
 */
public class W3CProvActivity implements W3CProvObject {

    private UUID uuid;
    private ProvActivity activity;
    private String name;
    private String value;
    private long ts;

    public W3CProvActivity(UUID uuid, ProvActivity activity, String name,
            String value) {
        super();
        this.uuid = uuid;
        this.activity = activity;
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

    public ProvActivity getActivity() {
        return activity;
    }

    public void setActivity(ProvActivity activity) {
        this.activity = activity;
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
        return "activity(" + uuid.toString() + ", [name=\"" + name + "\", ts=\""
                + String.valueOf(ts) + "\"])";
    }

    @Override
    public String toJson() {
        return "{\"w3cProvType\":\"activity\",\"uuid\":\"" + uuid.toString()
                + "\",\"name\":\"" + name + "\",\"value\":\"" + value
                + "\",\"ts\":\"" + String.valueOf(ts) + "\"}";
    }

}
