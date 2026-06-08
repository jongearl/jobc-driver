/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frotoma.jobc.builder;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 *
 * @author jongearl
 */
public class ParamBuilder {
    
    public static URL createURL( String urlString ){
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {}
        return url;
    }
    
    public static IRI createIRI( String iriString){
        IRIFactory iriFactory = IRIFactory.iriImplementation();
        return iriFactory.create(iriString);
    }
    
    public static Literal createLiteral(String str) {
        return ResourceFactory.createPlainLiteral(str);
    }
    
    public static Literal createLiteral(String str, String en) {
        return ResourceFactory.createLangLiteral(str, en);
    }
    
    public static Literal createLiteral(Integer obj) {
        return ResourceFactory.createTypedLiteral(obj);
    }
    
    public static Literal createLiteral(Double obj) {
        return ResourceFactory.createTypedLiteral(obj);
    }
    
    public static Literal createLiteral(Float obj) {
        return ResourceFactory.createTypedLiteral(obj);
    }
    
    public static Literal createLiteral(Boolean obj) {
        return ResourceFactory.createTypedLiteral(obj);
    }
    
    public static Literal createLiteral(URL uri) {
        return ResourceFactory.createTypedLiteral(uri);
    }
    
    public static Literal createLiteral(Date date) {
        return ResourceFactory.createTypedLiteral(date);
    }
    
    public static Literal createTypedLiteral(String str, XSDDatatype type) {
        return ResourceFactory.createTypedLiteral(str, type);
    }
}
