package edu.mit.ll.provsdnanalysis;

import java.util.HashMap;

public class W3CProvGraphNode {

    private String uuid;
    private String type;
    private String value; // raw string from JSON
    private String ts;
    private HashMap<String, String> dict; // key-value pairs from raw JSON

    public W3CProvGraphNode(String uuid, String type, String value, String ts) {
        super();
        this.uuid = uuid;
        this.type = type;
        this.value = value;
        this.ts = ts;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public void putKeyValue(String k, String v) {
        this.dict.put(k, v);
    }

    public String getValueByKey(String k) {
        return this.dict.get(k);
    }

    public Boolean containsKey(String k) {
        return this.dict.containsKey(k);
    }

    public Boolean containsValue(String v) {
        return this.dict.containsValue(v);
    }

}
