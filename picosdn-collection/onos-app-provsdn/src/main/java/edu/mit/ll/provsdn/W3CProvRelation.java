package edu.mit.ll.provsdn;

/**
 * W3C PROV relation class representation.
 *
 * @author Benjamin Ujcich <benjamin.ujcich@ll.mit.edu> <ujcich2@illinois.edu>
 * @author Samuel Jero <samuel.jero@ll.mit.edu>
 * @version 2.0
 */
public class W3CProvRelation {

    private W3CProvObject from;
    private W3CProvObject to;
    private W3CProvRelationType type;
    private long ts;

    public W3CProvRelation(W3CProvObject from, W3CProvObject to,
            W3CProvRelationType type) {
        super();
        this.from = from;
        this.to = to;
        this.type = type;
        this.ts = System.currentTimeMillis();
    }

    public W3CProvObject getFrom() {
        return from;
    }

    public W3CProvObject getTo() {
        return to;
    }

    public W3CProvRelationType getType() {
        return type;
    }

    public String toProvN() {
        return getTypeString() + "(" + from.getUuid().toString() + ", "
                + to.getUuid().toString() + ")";
    }

    public String toJson() {
        return "{\"w3cProvType\":\"" + getTypeString() + "\",\"from\":\""
                + from.getUuid().toString() + "\",\"to\":\""
                + to.getUuid().toString() + "\",\"ts\":\"" + String.valueOf(ts)
                + "\"}";
    }

    private String getTypeString() {
        switch (type) {
        case USED:
            return "used";
        case ACTED_ON_BEHALF_OF:
            return "actedOnBehalfOf";
        case WAS_ASSOCIATED_WITH:
            return "wasAssociatedWith";
        case WAS_DERIVED_FROM:
            return "wasDerivedFrom";
        case WAS_GENERATED_BY:
            return "wasGeneratedBy";
        case WAS_INFORMED_BY:
            return "wasInformedBy";
        case WAS_REVISION_OF:
            return "wasRevisionOf";
        case INVALIDATES:
            return "invalidates";
        default:
            return "";
        }
    }

}
