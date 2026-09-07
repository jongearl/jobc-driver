package com.frotoma.jobc.obj;

public class TripleObj {

    private ResourceObj subject = null;
    private ResourceObj predicate = null;   
    private ResourceObj object = null;
    private LiteralObj literal = null;

    public TripleObj(ResourceObj subject, ResourceObj predicate, ResourceObj object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public TripleObj(ResourceObj subject, ResourceObj predicate, LiteralObj literal) {
        this.subject = subject;
        this.predicate = predicate;
        this.literal = literal;
    }

    public ResourceObj getSubject() {
        return subject;
    }

    public ResourceObj getPredicate() {
        return predicate;
    }

    public ResourceObj getObject() {
        return object;
    }

    public LiteralObj getLiteral() {
        return literal;
    }


    public String toString() {
        String tripleStr = null;
        if( object != null ){
            tripleStr = subject.toString() + " " + predicate.toString() + " " + object.toString();
        }else if( literal != null ){
            tripleStr = subject.toString() + " " + predicate.toString() + " " + literal.toString();
        }
        return "\t"+tripleStr;
    }


}
