package com.frotoma.jobc.obj;

public class BNodeObj {

    private String id = null;

    public BNodeObj(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String toString() {
        return "_:"+id;
    }

}
