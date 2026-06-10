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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.iri.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PreparedStatement에 파라메터 저장하기
 */
public class PrepareQueryBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(PrepareQueryBuilder.class);


    private String sql = null;

    private List<Object> values;


    public PrepareQueryBuilder(String sql){
        this.sql = sql;        
        this.values = new ArrayList<>();
    }

    public void addParameter( int idx , Object obj){
        String val = null;
        if( obj instanceof String ){
            val = "\""+ obj +"\"";
        }else if( obj instanceof Date ){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            val = "\""+sdf.format(obj)+"\"";
        }else if( obj instanceof IRI){
            val = "<"+ obj +">";
        }else if( obj instanceof URL){    
            val = "\""+ obj +"\"";
        }else {
            val = obj.toString();
        }
        values.add(idx-1, val);
    }

    public String query(){
        String _sql = sql;        
        for( Object val : values ){
            _sql = _sql.replaceFirst("\\s\\?\\s|\\s\\?$", " "+ val +" ");
        }
        return _sql;
    }

    public void clearParameters() {
        values = new ArrayList<>();
    }

    private void test(){

        String _sql = sql;

        Pattern p = Pattern.compile("\\s\\?\\s|\\s\\?$");

        int count = 0;
        Matcher m = p.matcher(_sql);
        while( m.find()){ 
            logger.debug(m.group() );
            count++; 
        }

        logger.debug( ""+count );

    }


    public static void main(String[] args){
        String sql = " SELECT ?a WHERE { ? pred ? } limit ? ? ? ?";
        PrepareQueryBuilder box = new PrepareQueryBuilder(sql);
        box.test();

        box.addParameter(1, "subj");
        box.addParameter(2, "obj");
        box.addParameter(3, "10");
        box.addParameter(4, "11");
        box.addParameter(5, "12");
        box.addParameter(6, "13");

        logger.debug( box.query() );


    }

    
    
    
}
