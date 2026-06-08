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
