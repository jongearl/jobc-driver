package com.frotoma.onto.pool;

import com.frotoma.test.WebDriverTest;
import com.frotoma.jobc.builder.ParamBuilder;
import com.frotoma.jobc.web.WebSparqlStatement;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VirtTest {
    
    public void statementTest(){

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            DataSource dataSource = HikariDataSourceFactory2.getDataSource();            
            conn = dataSource.getConnection();            
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);            

            System.out.println("The Connection Object is of Class: " + conn.getClass());

            int i  =  0;
            while (rs.next()) {
                System.out.println( (i++) +"\t"+ rs.getString(1) + ", " + rs.getString(2) );
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

    public void preparedStatementTest(){

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            DataSource dataSource = HikariDataSourceFactory2.getDataSource();
            System.out.println( dataSource );
            conn = dataSource.getConnection();            
            System.out.println( conn.toString() );
            stmt = conn.prepareStatement(preparedSql);

//            URL url = new URL("http://www.w3.org/2004/02/skos/core#altLabel");
//            stmt.setURL(1, url);
            stmt.setObject(1, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Test5") );
    

            rs = stmt.executeQuery();            

            while (rs.next()) {
                System.out.println(rs.getString(1) + "," + rs.getString(2) );
            }
            
            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
            if( rs != null ){ try { rs.close(); } catch (SQLException ignore) {} }
            if( stmt != null ){ try { stmt.close(); } catch (SQLException ignore) {} }
            if( conn != null ){ try { conn.close(); } catch (SQLException ignore) {} }
            
        }

    }

    public static void main(String[] args) {
        System.setProperty("dbpool.config", "conf/dbpool.properties");
        System.setProperty("log4j.configurationFile", "conf/log4j2.xml");
        
        Logger logger = LoggerFactory.getLogger(WebDriverTest.class);
        logger.info("시작");        
        
        VirtTest m = new VirtTest();
//        m.statementTest();
        m.preparedStatementTest();        
    }

    private static String sql = "SPARQL SELECT DISTINCT *\n" +
"FROM <http://opendata.mofa.go.kr/mofaservice> \n" +
"WHERE {   \n" +
"  FILTER( ?uriname = \"홍콩\" ).\n" +
"  ?uri <http://www.w3.org/2004/02/skos/core#altLabel>  ?uriname .        \n" +
"  ?uri <http://opendata.mofa.go.kr/mofabrief/relatedBriefing> ?doc. \n" +
"  ?doc <http://www.w3.org/2004/02/skos/core#prefLabel> ?docname .     \n" +
"}  \n" +
"ORDER BY ?docname  LIMIT 10"; 

    private static String preparedSql = "SELECT *\n" +
"FROM <http://localhost:8890/DAV> \n" +
"WHERE {   \n" +
"  FILTER( ?doc = ? ).\n" +
"  ?doc ?p ?o .     \n" +
"}  \n" +
"ORDER BY ?docname  LIMIT 10"; 
    
}
