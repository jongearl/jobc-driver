package com.frotoma.virt.test;

import com.frotoma.test.*;
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
 * Unit test for simple App.
 */
public class VirtuosoPstmtTest {
        
    private static final String DRIVER = "com.frotoma.jobc.JobcDriver";
    private static final String URL = "jobc:virtuoso://localhost:1111/CHARSET=UTF-8";
    private static final String USER ="dba";
    private static final String PASSWORD ="dba";
    
    
    public void insertDataTest(){
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph ? { ? a ? } }";

        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
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


    String sql = "SELECT * WHERE { graph <http://localhost:8890/DAV> { <http://wwww.frotoma.com/lod/resource/Agent1> a ?t }}";
        
    /**
     * Rigorous Test :-)
     */
    public void selectAgent1()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        // String sql = "SELECT DISTINCT ?g WHERE { graph ?g { ?i a ?t }}";
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
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



    String sql1 = "SELECT * WHERE { graph <http://localhost:8890/DAV> { ?i a <http://xmlns.com/foaf/0.1/Agent> }}";
        
    /**
     * Rigorous Test :-)
     */
    public void selectAllAgent()
    {
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        // String sql = "SELECT DISTINCT ?g WHERE { graph ?g { ?i a ?t }}";
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql1);            
            ResultSetMetaData rsmd = rs.getMetaData();            
            for( int i = 0; i < rsmd.getColumnCount(); i++ ){
                System.out.println( i +" : "+rsmd.getColumnLabel(i+1) );
            }
            System.out.println("====================================");
            while( rs.next() ){
                for( int i = 1; i <= rsmd.getColumnCount(); i++ ){
                    String label = rsmd.getColumnLabel(i);
                    Object obj = rs.getObject( i ) ;
                    System.out.println( label +" : "+ obj );
                }                
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }
    
    
    public void selectAllOrganization_pstmt()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String sql = "SELECT * WHERE { graph ? { ?i a ? }}";
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);                                    
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            System.out.println(pstmt);
            
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));               
            pstmt.setObject(2, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Organization"));
            
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
            }
            rs.close();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }
    

    public void updateAgent10(){

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph<http://localhost:8890/DAV> { <http://wwww.frotoma.com/lod/resource/Agent10> a <http://xmlns.com/foaf/0.1/Agent>} }";
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);            
            
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
    
    
    
    public void updateAgent100_pstmt(){
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph ? { ? a ? } }";

        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Agent100"));
            pstmt.setObject(3, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
            
            int i = pstmt.executeUpdate();
            //Statement stmt = conn.createStatement();
            //int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );

            conn.commit();
            
            //stmt.close();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       

    }
    
    
    public void updateAgent0Name_pstmt(){
        
        String insert = "INSERT DATA { graph ? { ? <http://www.w3.org/2000/01/rdf-schema#label> ? } }";
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Agent0"));            
            pstmt.setObject(3, ParamBuilder.createLiteral("에이전트0", "ko"));            
            
            int i = pstmt.executeUpdate();
            //Statement stmt = conn.createStatement();
            //int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );

            conn.commit();
            
            //stmt.close();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       

    }
    
    public void deleteAgent0Name_pstmt(){
        
        String insert = "DELETE DATA { graph ? { ? <http://www.w3.org/2000/01/rdf-schema#label> ? } }";
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Agent0"));             
            pstmt.setObject(3, ParamBuilder.createLiteral("에이전트0", "ko"));            
            
            int i = pstmt.executeUpdate();
            //Statement stmt = conn.createStatement();
            //int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );

            conn.commit();
            
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
        String dropSparql = "DROP SILENT graph ?";

        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement(dropSparql);
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));            
            
            int i = pstmt.executeUpdate();
            //Statement stmt = conn.createStatement();
            //int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );

            conn.commit();
            
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
        
        Logger logger = LoggerFactory.getLogger(VirtuosoPstmtTest.class);
        logger.info("시작");        
        
        VirtuosoPstmtTest m = new VirtuosoPstmtTest();
//        m.insertDataTest();
        //m.selectAgent1();
        m.selectAllAgent();
        //m.selectAllOrganization_pstmt();
        //m.updateAgent10();
        //m.updateAgent100_pstmt();
        //m.updateAgent0Name_pstmt();
        //m.deleteAgent0Name_pstmt();
//        m.DropTest();
    }
}
