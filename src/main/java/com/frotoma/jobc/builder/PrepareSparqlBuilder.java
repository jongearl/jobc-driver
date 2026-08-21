/*
MIT License

Copyright (c) 2026 jongearl

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
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
