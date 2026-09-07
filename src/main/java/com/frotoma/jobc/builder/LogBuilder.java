package com.frotoma.jobc.builder;

/**
 *
 * @author jongearl
 */
public class LogBuilder {
    
    public static String generateOneline(String sparql){
        String oneline = sparql;
        if( sparql != null ){
            oneline = sparql.replaceAll("\n|\r", " ");
        }
        return oneline;
    }
    
}
