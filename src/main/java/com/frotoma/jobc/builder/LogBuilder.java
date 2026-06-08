/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
