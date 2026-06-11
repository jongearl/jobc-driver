package com.frotoma.onto.pool;

import com.frotoma.jobc.builder.ParamBuilder;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;


public class HikariTest {

    public static void drop(){

        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {            
            conn = HikariDataSourceFactory2.getDataSource().getConnection();
            pstmt = conn.prepareStatement("DROP SILENT GRAPH <http://localhost:8890/DAV>");
            int cnt = pstmt.executeUpdate();  

            conn.commit();

            pstmt.close();
            conn.close();
                      
        } catch (SQLException e) {            
            e.printStackTrace();
        }finally{
            if( pstmt != null ){ try { pstmt.close(); } catch (SQLException ignore) {} }
            if( conn != null ){ try { conn.close(); } catch (SQLException ignore) {} }
        }
    }

    public static void statementTest(){

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            DataSource dataSource = HikariDataSourceFactory2.getDataSource();
            conn = dataSource.getConnection();            
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);            

            while (rs.next()) {
                System.out
                        .println(rs.getString(1) );
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            if( rs != null ){ try { rs.close(); } catch (SQLException ignore) {} }
            if( stmt != null ){ try { stmt.close(); } catch (SQLException ignore) {} }
            if( conn != null ){ try { conn.close(); } catch (SQLException ignore) {} }
            e.printStackTrace();
        }


    }

    public static void preparedStatementTest(){

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            DataSource dataSource = HikariDataSourceFactory2.getDataSource();            
            conn = dataSource.getConnection();            
            pstmt = conn.prepareStatement(preparedSql);

            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));               
            pstmt.setObject(2, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
            
            rs = pstmt.executeQuery();            

            while (rs.next()) {
                System.out
                        .println(rs.getString(1) );
            }

            rs.close();
            pstmt.close();
            conn.close();

        } catch (Exception e) {
            if( rs != null ){ try { rs.close(); } catch (SQLException ignore) {} }
            if( pstmt != null ){ try { pstmt.close(); } catch (SQLException ignore) {} }
            if( conn != null ){ try { conn.close(); } catch (SQLException ignore) {} }
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        System.setProperty("log4j.configurationFile", "conf/log4j2.xml");
        System.setProperty("dbpool.config", "conf/dbpool.properties");
        
        drop();
//        statementTest();
         //preparedStatementTest();        
    }

    private static final String sql = "SELECT * "
                + "WHERE { \n"
                + "graph <http://localhost:8890/DAV> { "
                + "?i a <http://xmlns.com/foaf/0.1/Agent> "
                + "} "
                + "}";

    private static final String preparedSql = "SELECT *  WHERE {  graph ?  {  ?i a ? }}";  

}
