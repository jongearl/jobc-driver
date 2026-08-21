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
public class FusekiTest {
    
    private static final String DRIVER = "com.frotoma.jobc.fuseki.FusekiSparqlDriver";
    private static final String URL = "jobc:fuseki:http://localhost:3030/jobc/sparql&method=post";
    String user ="admin";
    String password ="1234";
    
    public void insertSampleData(){
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph ? { ? a ? } }";

        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            
            for( int i = 0; i< 10 ; i++){
                pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
                pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Agent"+i));            
                pstmt.setObject(3, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
                
                pstmt.addBatch();
            }           
            
            for( int i = 10; i< 20 ; i++){
                pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
                pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/ORG"+i));            
                pstmt.setObject(3, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Organization"));
                pstmt.addBatch();
            }
            
            int[] result = pstmt.executeBatch();
            
            for( int r : result ){
                System.out.println("batch result : " + r);
            }
            conn.commit();

            //stmt.close();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
    }
        
    public void selectAllAgent()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String sql = "SELECT * \n"
                + "WHERE { \n"
                + "graph <http://localhost:8890/DAV> { \n"
                + "?s ?p ?o . \n"
                + "}\n"
                + "}";
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
    
    
    public void selectAllAgent_pstmt()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String sql = "SELECT *  WHERE {  graph ?  {  ?i a ? }}";
        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));               
            pstmt.setObject(2, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
            
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
    
    
    public void updateAgent0Name(){

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph<http://localhost:8890/DAV> { <http://wwww.frotoma.com/lod/resource/Agent0> <http://www.w3.org/2000/01/rdf-schema#label> 'Agent0_Name' } }";
        try {
            Connection conn = DriverManager.getConnection(URL, user, password);            
            
            Statement stmt = conn.createStatement();
            int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );

            conn.commit();
            
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        

    }
    
    
    public void updateAgent10_pstmt(){
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph ? { ? a ? } }";

        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Agent10"));            
            pstmt.setObject(3, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
            
            int i = pstmt.executeUpdate();
            //Statement stmt = conn.createStatement();
            //int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );
            
            //stmt.close();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       

    }
    
    
    public void updateAgent0Rel_pstmt(){
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph ? { ? <http://wwww.frotoma.com/lod/resource/rel> ? } }";

        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Agent0"));            
            pstmt.setObject(3, ParamBuilder.createLiteral("PUHAHA", "ko"));            
            
            int i = pstmt.executeUpdate();
            //Statement stmt = conn.createStatement();
            //int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );
            
            //stmt.close();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       

    }
    
    public void deleteAgent1_pstmt(){
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "DELETE DATA { graph ? { ? a ? } }";

        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Agent1"));
            pstmt.setObject(3, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
            
            int i = pstmt.executeUpdate();
            //Statement stmt = conn.createStatement();
            //int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );
            
            //stmt.close();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
    }
    
    
    public void DropTest(){
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "DROP SILENT graph ?";

        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));            
            
            int i = pstmt.executeUpdate();
            //Statement stmt = conn.createStatement();
            //int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );
            
            //stmt.close();
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
        
        FusekiTest m = new FusekiTest();
        m.insertSampleData();
        // m.updateAgent0Name();
        // m.updateAgent10_pstmt();
        //m.updateAgent0Rel_pstmt();
        // m.selectAllAgent();
        // m.selectAllAgent_pstmt();        
        // m.deleteAgent1_pstmt();
        //m.DropTest();
    }
}
