/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frotoma.jobc.builder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.jena.iri.IRI;

/**
 *
 * @author jongearl
 */
public class VirtuosoPreparedSparqlBuilder {
    
    
    private String sparqlQuery;
    
    // Column IRI or Object , IRI = TRUE, Object FALSE
    private Map<Integer,Boolean> columnHeadertype = new HashMap<>();
    
    public void setQuery( String sparqlQuery ){
        this.sparqlQuery = sparqlQuery;
    }
    
    public void addParameter(int idx , Object obj){        
        if( this.columnHeadertype.get(idx) != null ) return;        
        if( obj instanceof IRI){
            this.columnHeadertype.put(idx, Boolean.TRUE);
        }else{
            this.columnHeadertype.put(idx, Boolean.FALSE);
        }        
    }
    
    
    
    public String query() throws SQLException{        
        
        String virtuosoSparqlQuery = sparqlQuery + " ";
        
        int countParameter = countParameter4Sparql();
        
        if( countParameter == this.columnHeadertype.size() ){            
            int idx = 1;
            while( virtuosoSparqlQuery.contains(" ? ")){                
                if( this.columnHeadertype.get(idx) ){ // IRI인 경우
                    virtuosoSparqlQuery = virtuosoSparqlQuery.replaceFirst("\\s\\?\\s", " iri(??) ");
                }else{
                    virtuosoSparqlQuery = virtuosoSparqlQuery.replaceFirst("\\s\\?\\s", " ?? ");
                }
                idx++;
            }
        }else{
            throw new SQLException("Not match Parameter " + countParameter +" != "+ this.columnHeadertype.size() );
        }
        
        virtuosoSparqlQuery = "SPARQL " + virtuosoSparqlQuery;
        
        return virtuosoSparqlQuery;
    }
    
    
    private int countParameter4Sparql(){        
        String text = sparqlQuery;
        String target = " ? ";
        return (text.length() - text.replace(target, "").length()) / target.length();
    }
    
    
    
}
