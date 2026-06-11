package com.frotoma.onto.pool;

import com.frotoma.jobc.builder.ParamBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.jena.sparql.function.library.leviathan.pow;


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

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            DataSource dataSource = crateDataSource();            
            conn = dataSource.getConnection();            
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);            

            while (rs.next()) {
                System.out.println(rs.getString(1) + "\t" + rs.getString(2) );
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
            DataSource dataSource = crateDataSource();
            conn = dataSource.getConnection();            
            pstmt = conn.prepareStatement(preparedSql);

            pstmt.setObject(1, ParamBuilder.createIRI("http://dbpedia.org/resource/BTS"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://dbpedia.org/ontology/bandMember") );
            //stmt.setString(2, "South Korea");
            
            rs = pstmt.executeQuery();            
            
            ResultSetMetaData rsm = pstmt.getMetaData();
            int columnCount = rsm.getColumnCount();
            
            
            System.out.println("============================================== ");
            
            System.out.print("NUM");
            for( int i = 0; i< columnCount; i++ ){
                System.out.print( "\t" );
                System.out.print( rsm.getColumnLabel(i+1) );                    
            }
            System.out.println("");            
            System.out.println("---------------------------------------------- ");

            int num = 0;
            while (rs.next()) {
                System.out.print( num );
                for( int i = 0; i< columnCount; i++ ){
                    System.out.print( "\t" );
                    System.out.print( rs.getString(i+1) );
                }
                System.out.println("");
                num++;
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
        //statementTest();                
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
"SELECT * " +
" WHERE {  " +
"  Filter( ?person = ? ) . " +
"  ?person ? ?member . " +
"  ?member <http://dbpedia.org/property/name> ?membername ." +
"}  ";
    
}
