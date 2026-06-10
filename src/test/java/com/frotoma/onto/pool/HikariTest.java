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
        } catch (SQLException e) {            
            e.printStackTrace();
        }finally{
            if( conn != null ){
                try {
                    conn.close();
                } catch (SQLException e) {}
            }
        }
    }

    public static void statementTest(){

        Connection connection = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        try {
            DataSource dataSource = HikariDataSourceFactory2.getDataSource();
            connection = dataSource.getConnection();            
            stmt = connection.createStatement();
            resultSet = stmt.executeQuery(sql);            

            while (resultSet.next()) {
                System.out
                        .println(resultSet.getString(1) );
            }

        } catch (Exception e) {
            try {
                connection.close();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }


    }

    public static void preparedStatementTest(){

        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            DataSource dataSource = HikariDataSourceFactory2.getDataSource();            
            connection = dataSource.getConnection();            
            pstmt = connection.prepareStatement(preparedSql);

            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));               
            pstmt.setObject(2, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
            
            resultSet = pstmt.executeQuery();            

            while (resultSet.next()) {
                System.out
                        .println(resultSet.getString(1) );
            }

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
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
