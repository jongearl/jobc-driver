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
 * Unit test for simple App.
 */
public class OriginalVirtDriverTest {
        
    private static final String DRIVER = "virtuoso.jdbc4.Driver";
    private static final String url = "jdbc:virtuoso://localhost:1111/CHARSET=UTF-8";
    private static final String user ="dba";
    private static final String password ="dba";


    String sql = "SELECT * WHERE { graph <http://localhost:8890/DAV> { <http://wwww.frotoma.com/lod/resource/Test1> a ?t }}";
        
    /**
     * Rigorous Test :-)
     */
    public void select()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        // String sql = "SELECT DISTINCT ?g WHERE { graph ?g { ?i a ?t }}";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
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
    public void select1()
    {
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        // String sql = "SELECT DISTINCT ?g WHERE { graph ?g { ?i a ?t }}";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
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
    
    /**
     * FROM 절에서 URI는 파라메터로 받을 수 없음
     */
    public void selectPrepared()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        //String sql = "sparql SELECT * WHERE { graph `iri(??)` { ?i a `iri(??)` }}";
        
        String sql1 = "SPARQL SELECT *\n" +                
                "FROM <http://localhost:8890/DAV> " + // DO NOT USE PARAMETER
                "WHERE {   " +                                
                "  FILTER( ?o = IRI(??) ).\n" +
                "  ?doc ?p ?o ." +
                "} " +
                "LIMIT 10";        
        
        try {
            Connection conn = DriverManager.getConnection(url, user, password);                                    
            PreparedStatement pstmt = conn.prepareStatement(sql1);
            
            System.out.println(pstmt);
            
            //pstmt.setString(1, "http://localhost:8890/DAV");
            pstmt.setString(1, "http://xmlns.com/foaf/0.1/Agent");
            
            ResultSet rs = pstmt.executeQuery();
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
    

    public void updateTrue(){

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph<http://localhost:8890/DAV> { <http://wwww.frotoma.com/lod/resource/Test6> a <http://xmlns.com/foaf/0.1/Agent>} }";
        try {
            Connection conn = DriverManager.getConnection(url, user, password);            
            
            Statement stmt = conn.createStatement();
            int i = stmt.executeUpdate(insert);

            System.out.println( "결과 : "+i );
            
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        

    }
    
    
    
    public void updatePreparedTrue(){
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "SPARQL INSERT INTO graph iri(??) { `iri(??)` <http://wwww.frotoma.com/lod/resource/relInt> ?? } ";

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            pstmt.setString(1, "http://localhost:8890/DAV");
            pstmt.setString(2, "http://wwww.frotoma.com/lod/resource/Test1");
            pstmt.setInt(3, 12);            
            
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
        
        Logger logger = LoggerFactory.getLogger(OriginalVirtDriverTest.class);
        logger.info("시작");        
        
        OriginalVirtDriverTest m = new OriginalVirtDriverTest();
//        m.select();
//        m.select1();
        m.selectPrepared();
//        m.updateTrue();
//        m.updatePreparedTrue();
    }
}
