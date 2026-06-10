package com.frotoma.onto.sparql;


import com.frotoma.jobc.rest.APISelectPostService;
import com.frotoma.onto.pool.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PareparedUpdateSparqlTest {
    
    private Logger logger = LoggerFactory.getLogger(PareparedUpdateSparqlTest.class);

    private BasicDataSource bds = null;

    public PareparedUpdateSparqlTest(){

        // 관리시스템 (작동)
//        String url = "jdbc:http://192.168.10.19:8096/sparql&method=post";
//        String user ="master";
//        String password ="1234";
        
       
        // JENA (정상 작동)
        String url = "jdbc:http://localhost:3030/mofadocu/sparql&method=post";
        String user ="admin";
        String password ="1234";
        
        // virtuoso condcutor web (정상작동)
//        String url = "jdbc:http://192.168.10.111:8890/sparql&method=post";
//        String user ="dba";
//        String password ="dba";

        bds = new BasicDataSource();
        
        // DBMS 계정 및 초기 설정
        bds.setDriverClassName("com.frotoma.jdbc.fuseki.FusekiSparqlDriver");
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


    /**
     * 한개의 소스로 다양한 온톨로지 저장소 쿼리를 수행
     */
    public void selectTrue(){
        // String sql = "SELECT DISTINCT ?g WHERE { graph ?g { ?i a ?t }}";
        try {
            Connection conn = getConnection();            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            int i = pstmt.executeUpdate();
                        
            
                logger.info( " >> "+i  );
            
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

        PareparedUpdateSparqlTest d = new PareparedUpdateSparqlTest();
        d.selectTrue();

    }


private static String sql = 
"INSERT DATA\n" +
"{ GRAPH <http://opendata.mofa.go.kr/mofaservice>" +
"  { <http://example/book1> dc:title \"A new book\" ;\n" +
"                         dc:creator \"A.N.Other\" . }" +
"}" ;   
        
    
}
