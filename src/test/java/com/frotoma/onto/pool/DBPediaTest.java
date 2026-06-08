package com.frotoma.onto.pool;

import com.frotoma.jobc.builder.ParamBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;


public class DBPediaTest {
    
    public static DataSource crateDataSource(){
        String url =    "jobc:web:http://dbpedia.org/sparql/";
        // String url = "jdbc:virtuoso://192.168.10.4:1111/CHARSET=UTF-8";
        String user = null;
        String password = null;

        HikariConfig config = new HikariConfig();

        config.setDriverClassName("com.frotoma.jobc.web.WebSparqlDriver");                                    
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);

        config.setMaximumPoolSize(2);
        config.setAutoCommit(false);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setValidationTimeout(3000);            

        return new HikariDataSource(config);
    }

    public static void statementTest(){

        Connection connection = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        try {
            DataSource dataSource = crateDataSource();
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
                connection.rollback();
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
            DataSource dataSource = crateDataSource();
            connection = dataSource.getConnection();            
            stmt = connection.prepareStatement(preparedSql);

            stmt.setObject(1, ParamBuilder.createIRI("http://dbpedia.org/resource/BTS"));
            stmt.setString(2, "South Korea");
            
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
//        statementTest();                
        preparedStatementTest();
    }

    private static String sql = 
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

    private static String preparedSql = 
"SELECT ?person ?value\n" +
" WHERE {     \n" +
"  Filter( ?person = ? ) .     \n" +
"  ?person <http://dbpedia.org/property/origin> ?value .\n" +
"  FILTER ( CONTAINS( STR( ?value)  , ? ) )\n" +
"}  ";
    
}
