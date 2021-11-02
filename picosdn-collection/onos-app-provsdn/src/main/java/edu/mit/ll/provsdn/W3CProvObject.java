package edu.mit.ll.provsdn;

import java.util.UUID;

/**
 * W3C PROV object class representation.
 *
 * @author Benjamin Ujcich <benjamin.ujcich@ll.mit.edu> <ujcich2@illinois.edu>
 * @author Samuel Jero <samuel.jero@ll.mit.edu>
 * @version 2.0
 */
public interface W3CProvObject {

    public UUID getUuid();

    public void setUuid(UUID uuid);

    public String getName();

    public void setName(String name);

    public String getValue();

    public void setValue(String value);

    public String toProvN();

    public String toJson();

}