package com.frotoma.jobc.obj;

public class ResourceObj {

    private String uri = null;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public ResourceObj( String uri ){
        this.uri = uri;
    }    

    public String toString(){
        return uri;
    }
    
}
