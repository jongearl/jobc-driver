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
 *
 * @author jongearl
 */
public class testRDF12 {
    
    private static final String DRIVER = "com.frotoma.jobc.fuseki.FusekiSparqlDriver";
    private static final String URL = "jobc:fuseki:http://localhost:3030/jobc/sparql&method=post";
    String user ="admin";
    String password ="1234";
    
    

    public void selectRDF12Agent()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String sql = "PREFIX : <http:exmaple.org> SELECT * \n"
                + "WHERE { \n"                
                + "?s ?p ?o . \n"
                + "}\n"
                + " LIMIT 10";
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
    
    
    public static void main(String[] args){
        System.setProperty("log4j.configurationFile", "conf/log4j2.xml");
        
        Logger logger = LoggerFactory.getLogger(WebDriverTest.class);
        logger.info("시작");        
        
        testRDF12 m = new testRDF12();
        // m.insertSampleData();
        // m.updateAgent0Name();
        // m.updateAgent10_pstmt();
        //m.updateAgent0Rel_pstmt();
        //m.selectAllAgent();
         //m.selectAllAgent_pstmt();        
        // m.deleteAgent1_pstmt();
        //m.DropTest();

        m.selectRDF12Agent();
    }
}
