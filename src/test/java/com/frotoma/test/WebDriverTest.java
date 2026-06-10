package com.frotoma.test;

import com.frotoma.jobc.builder.ParamBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DBPedia Test
 * @author jongearl
 */
public class WebDriverTest {
    
    private static final String DRIVER = "com.frotoma.jobc.JobcDriver";
    private static final String URL = "jobc:web:http://dbpedia.org/sparql/";
    String user ="admin";
    String password ="1234";
        
    public void select()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        
        String sql = 
            "SELECT distinct ?person ?name " +
            "WHERE { " +
            "    { " +
            "    SELECT ?person ?name" +
            "    WHERE { " +
            "      ?person <http://www.w3.org/2000/01/rdf-schema#label> ?name ." +
            "      ?person <http://dbpedia.org/property/birthPlace> ?value ." +
            "      ?person <http://dbpedia.org/property/occupation> 'Singer'@en" +
            "      FILTER ( CONTAINS( STR( ?value)  , \"South Korea\" ) )" +
            "    }" +
            "    }" +
            "    FILTER ( LANG(  ?name ) = \"en\" ) " +
            "}" +
            "limit 10";  
        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);            
            ResultSetMetaData rsmd = rs.getMetaData();            
            for( int i = 0; i < rsmd.getColumnCount(); i++ ){
                System.out.println( i +" : "+rsmd.getColumnLabel(i+1) );
            }
            System.out.println("====================================");
            while( rs.next() ){
                for( int i = 1; i <= rsmd.getColumnCount(); i++ ){
                    String label = rsmd.getColumnLabel(i);
                    Object obj = rs.getObject( label ) ;
                    System.out.println( label +" : "+ obj );
                }                
                System.out.println();
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }
    
    
    public void selectPrepared()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String preparedSql
                = "SELECT * "
                + " WHERE {  "
                + "  Filter( ?person = ? ) . "
                + "  ?person ? ?member . "
                + "  ?member <http://dbpedia.org/property/name> ?membername ."
                + "}  ";
        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            PreparedStatement pstmt = conn.prepareStatement(preparedSql);
            pstmt.setObject(1, ParamBuilder.createIRI("http://dbpedia.org/resource/BTS"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://dbpedia.org/ontology/bandMember") );
            
            ResultSet rs = pstmt.executeQuery();;
            ResultSetMetaData rsmd = rs.getMetaData();            
            for( int i = 0; i < rsmd.getColumnCount(); i++ ){
                System.out.println( i +" : "+rsmd.getColumnLabel(i+1) );
            }
            System.out.println("====================================");
            while( rs.next() ){
                for( int i = 1; i <= rsmd.getColumnCount(); i++ ){
                    String label = rsmd.getColumnLabel(i);
                    Object obj = rs.getObject( label ) ;
                    System.out.println( label +" : "+ obj );
                }
                System.out.println();
            }
            rs.close();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }
    
    
    
    public static void main(String[] args){
        System.setProperty("log4j.configurationFile", "conf/log4j2.xml");
        
        Logger logger = LoggerFactory.getLogger(WebDriverTest.class);
        logger.info("시작");        
        
        WebDriverTest m = new WebDriverTest();
        m.select();
        m.selectPrepared();
    }
}
