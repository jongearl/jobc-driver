package com.frotoma.onto.pool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class TestDrop {


    public void drop(){

        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {            
            conn = HikariDataSourceFactory2.getDataSource().getConnection();
            pstmt = conn.prepareStatement("DROP SILENT GRAPH <http://localhost:8890/DAV>");
            int cnt = pstmt.executeUpdate();            
            conn.commit();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            if( conn != null ){
                try {
                    conn.close();
                } catch (SQLException e) {}
            }
        }



    }

    public static void main(String[] args){
        
        System.setProperty("log4j.configurationFile", "conf/log4j2.xml");
        System.setProperty("dbpool.config", "conf/dbpool.properties");

        TestDrop TestDrop = new TestDrop();
        TestDrop.drop();

    }
    
}
