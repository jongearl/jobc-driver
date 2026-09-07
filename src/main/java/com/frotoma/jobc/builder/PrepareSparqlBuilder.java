package com.frotoma.jobc.builder;

import java.net.URL;
import java.util.Calendar;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.irix.IRIx;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Literal;

/**
 *
 * @author jongearl
 */
public class PrepareSparqlBuilder {
    
    ParameterizedSparqlString pss = null;
    
    public PrepareSparqlBuilder(String sql){          
        pss = new ParameterizedSparqlString();
        
        String sparql = sql + " ";
        pss.setCommandText(sparql);
        
        System.out.println("|"+sparql+"|");
    }
    
    public void addParameter( int idx , URL obj){        
        pss.setLiteral(getIndex(idx), obj.toString(), XSDDatatype.XSDanyURI);
    }
    
    public void addParameter( int idx , IRIx obj){        
        pss.setIri(getIndex(idx), obj);
    }
    
    public void addParameter( int idx , String obj){
        pss.setLiteral(getIndex(idx), (String)obj);
    }
    
    public void addParameter( int idx , Calendar obj){
        pss.setLiteral(getIndex(idx), (Calendar)obj);
    }
    
    public void addParameter( int idx , Literal obj){
        pss.setLiteral(getIndex(idx), (Literal)obj);
    }
    
    public void addParameter( int idx , Boolean obj){
        pss.setLiteral(getIndex(idx), obj);
    }
    
    public void addParameter( int idx , Double obj){
        pss.setLiteral(getIndex(idx), obj);
    }
    
    public void addParameter( int idx , Float obj){
        pss.setLiteral(getIndex(idx), obj);
    }
    
    public void addParameter( int idx , Integer obj){
        pss.setLiteral(getIndex(idx), obj);
    }
    
    public void addParameter( int idx , Long obj){
        pss.setLiteral(getIndex(idx), obj);
    }
    
    public void addParameter( int idx , Object obj){
        if( obj instanceof IRIx ){
            addParameter(idx, (IRIx)obj);        
        }else if( obj instanceof Literal ){    
            addParameter(idx, (Literal)obj);                
        }else{
            pss.setLiteral(getIndex(idx), (String)obj);        
        }
    }
    
    public String query(){        
        return pss.toString();
    }
    
    private int getIndex(int idx){
        return idx - 1;
    }
    
}
