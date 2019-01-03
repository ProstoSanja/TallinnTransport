package com.psanja.tallinntransport.DATAclasses;

import java.io.Serializable;

public class StopIDs implements Serializable {

    private String[] tltIDs;
    private String elronID;

    public StopIDs () {
        super();
    }

    public void setElronID(String elronID) {
        this.elronID = elronID;
    }

    public void setTltIDs(String[] tltIDs) {
        this.tltIDs = tltIDs;
    }

    public String getElronID() {
        return elronID;
    }

    public String[] getTltIDs() {
        return tltIDs;
    }

    public boolean isElron() {
        return elronID != null && !elronID.isEmpty();
    }

    public boolean isTlt() {
        return tltIDs != null && tltIDs.length > 0;
    }

    public int providers() {
        int result = 0;
        if (isElron())
            result++;
        if (isTlt())
            result++;
        return result;
    }
}
