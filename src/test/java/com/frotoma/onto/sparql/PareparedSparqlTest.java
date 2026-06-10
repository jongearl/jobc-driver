package com.frotoma.onto.sparql;


import com.frotoma.onto.pool.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbcp2.BasicDataSource;

public class PareparedSparqlTest {

    private BasicDataSource bds = null;

    public PareparedSparqlTest(){

      // 관리시스템 (작동)
//        String url = "jdbc:http://192.168.10.19:8096/sparql&method=post";
//        String user ="master";
//        String password ="1234";
        
        // LOD (정상작동)
        // TODO : CORS 설정 때문인지 확인하기
//        String url = "jdbc:http://localhost:8090/sparql&method=post";
//        String user = "master";
//        String password = "cmmsk$#@!";
        
        // JENA (정상 작동)
        String url = "jdbc:http://localhost:3030/mofaservice/sparql&method=post";
        String user ="admin";
        String password ="1234";
        
        // virtuoso condcutor web (정상작동)
//        String url = "jdbc:http://192.168.10.111:8890/sparql&method=post";
//        String user ="dba";
//        String password ="dba";



        bds = new BasicDataSource();
        
        // DBMS 계정 및 초기 설정
        bds.setDriverClassName("com.frotoma.jdbc.web.SparqlWebDriver");
        bds.setUsername(user);
        bds.setPassword(password);
        bds.setUrl(url);
        bds.setInitialSize(1);
        bds.setTestWhileIdle(false);        
        // bds.setTestWhileIdle(true);
        // bds.setValidationQuery("SELECT COUNT(?i) WHERE { ?i a ?t } LIMIT 1");
        // bds.setTimeBetweenEvictionRunsMillis(18000000);


    }

    private Connection getConnection() throws SQLException{
        return bds.getConnection();
    }


    public void selectTrue(){
        // String sql = "SELECT DISTINCT ?g WHERE { graph ?g { ?i a ?t }}";
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            System.out.println( pstmt );
            
            pstmt.setString(0, "홍콩");
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

    public static void main(String[] args){
        
        //System.setProperty("log4j.configurationFile", "conf/log4j.properties");        
        System.setProperty("log4j.configurationFile", "conf/log4j2.xml");

        PareparedSparqlTest d = new PareparedSparqlTest();
        d.selectTrue();

    }


    private static String sql = "SELECT *\n" +
"FROM <http://opendata.mofa.go.kr/mofaservice> \n" +
"WHERE {   \n" +
"  FILTER( ?uriname = ? ).\n" +
"  ?uri <http://www.w3.org/2004/02/skos/core#altLabel>  ?uriname .        \n" +
"  ?uri <http://opendata.mofa.go.kr/mofabrief/relatedBriefing> ?doc. \n" +
"  ?doc <http://www.w3.org/2004/02/skos/core#prefLabel> ?docname .     \n" +
"}  \n" +
"ORDER BY ?docname  LIMIT 10"; 
        
    
}
