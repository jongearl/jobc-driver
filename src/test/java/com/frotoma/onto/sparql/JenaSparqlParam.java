/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frotoma.onto.sparql;

import com.frotoma.jobc.builder.ParamBuilder;
import com.frotoma.jobc.builder.PrepareSparqlBuilder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author jongearl
 */
public class JenaSparqlParam {
    
    public static void main(String[] args){
        String input = "\"val\" }; DROP GRAPH <> {all";
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText("SELECT * WHERE { ?s ?p ?value }");
        
        pss.setLiteral("value", input);
        
        System.out.println(pss.toString());
        
        String qry = "SELECT * WHERE { ?s ?p "+input+ "}";
        System.out.println(qry);
        
        
        String sparql = "SELECT * WHERE { ?s ?p ? }";
        
        PrepareSparqlBuilder builder = new PrepareSparqlBuilder(sparql);        
        URL url = ParamBuilder.createURL("http://www.mofa.go.kr");
        builder.addParameter(0, url);
        
        
        
        System.out.println( builder.query() );
        
    }
}
