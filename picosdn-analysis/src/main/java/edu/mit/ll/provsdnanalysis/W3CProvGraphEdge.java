package edu.mit.ll.provsdnanalysis;

public class W3CProvGraphEdge {

    private String type;
    private String value;
    private String ts;

    public W3CProvGraphEdge(String type, String value, String ts) {
        super();
        this.type = type;
        this.value = value;
        this.ts = ts;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
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

}
