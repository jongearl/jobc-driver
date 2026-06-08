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
            System.out.println( dataSource );
            connection = dataSource.getConnection();            
            stmt = connection.createStatement();
            resultSet = stmt.executeQuery(sql);            

            System.out.println("The Connection Object is of Class: " + connection.getClass());

            while (resultSet.next()) {
                System.out
                        .println(resultSet.getString(1) + "," + resultSet.getString(2) );
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
        PreparedStatement stmt = null;
        ResultSet resultSet = null;
        try {
            DataSource dataSource = HikariDataSourceFactory2.getDataSource();
            System.out.println( dataSource );
            connection = dataSource.getConnection();            
            stmt = connection.prepareStatement(preparedSql);

            
            stmt.setObject(1, ParamBuilder.createIRI("http://www.w3.org/2004/02/skos/core#altLabel"));
            resultSet = stmt.executeQuery();            

            System.out.println("The Connection Object is of Class: " + connection.getClass());

            while (resultSet.next()) {
                System.out
                        .println(resultSet.getString(1) + "," + resultSet.getString(2) );
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
        
        // drop();
//        statementTest();
         preparedStatementTest();        
    }

    private static String sql = "SELECT ?doc ?docname "
    +" FROM <http://opendata.mofa.go.kr/mofaservice> "
    +"WHERE { "
        +"  ?uri <http://www.w3.org/2004/02/skos/core#altLabel>  ?uriname. "
        +" filter( ?uriname = \"외교부\" ) "
        +" ?uri <http://opendata.mofa.go.kr/mofabrief/relatedBriefing> ?doc. "
        +" ?doc <http://www.w3.org/2004/02/skos/core#prefLabel> ?docname . "
        +" } "
        +" ORDER BY ?docname "
        +" LIMIT 10"; 

    private static String preparedSql = "SELECT ?doc ?docname "
        +" FROM <http://opendata.mofa.go.kr/mofaservice> "
        +"WHERE { "
            +"  ?uri ?  ?uriname. "
            +" filter( ?uriname = \"한·미 정상회담\" ) "
            +" ?uri <http://opendata.mofa.go.kr/mofadocu/relatedDoc> ?doc. "
            +" ?doc <http://www.w3.org/2004/02/skos/core#prefLabel> ?docname . "
            +" } "
            +" ORDER BY ?docname "
            +" LIMIT 10";     

}
