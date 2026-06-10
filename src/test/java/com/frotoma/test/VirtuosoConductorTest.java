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
 * @author jongearl
 */
public class VirtuosoConductorTest {
    
    private static final String DRIVER = "com.frotoma.JobcDriver";
    private static final String URL = "jobc:web:http://localhost:8890/sparql/CHARSET=UTF-8";
    String user ="dba";
    String password ="dba";
        
    public void selectTrue()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String sql = "SELECT * WHERE { graph <http://localhost:8890/DAV> { ?i a <http://xmlns.com/foaf/0.1/Agent> }}";
        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
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
    
    
    public void selectPreparedTrue()
    {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String sql = "SELECT * WHERE { graph ? { ?i a ? }}";
        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            System.out.println(conn);
            int[] columnIndexes = {0};
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            System.out.println(pstmt);
            
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));               
            pstmt.setObject(2, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
            
            ResultSet rs = pstmt.executeQuery();;
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
    
    /**
     * 보안 에러 java.sql.SQLException: Virtuoso 42000 Error SR186:SECURITY: No permission to execute procedure
     */
    public void updateTrue(){

        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph<http://localhost:8890/DAV> { <http://wwww.frotoma.com/lod/resource/Test8> a <http://xmlns.com/foaf/0.1/Agent>} }";
        try {
            Connection conn = DriverManager.getConnection(URL, user, password);            
            
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
    
    /**
     * 보안 에러 java.sql.SQLException: Virtuoso 42000 Error SR186:SECURITY: No permission to execute procedure
     */
    public void updatePreparedTrue(){
        
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {            
            e.printStackTrace();
        }
        String insert = "INSERT DATA { graph ? { ? a ? } }";

        try {
            Connection conn = DriverManager.getConnection(URL, user, password);
            PreparedStatement pstmt = conn.prepareStatement(insert);
            
            pstmt.setObject(1, ParamBuilder.createIRI("http://localhost:8890/DAV"));
            pstmt.setObject(2, ParamBuilder.createIRI("http://wwww.frotoma.com/lod/resource/Test4"));            
            pstmt.setObject(3, ParamBuilder.createIRI("http://xmlns.com/foaf/0.1/Agent"));
            
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
        
        Logger logger = LoggerFactory.getLogger(WebDriverTest.class);
        logger.info("시작");        
        
        VirtuosoConductorTest m = new VirtuosoConductorTest();
//        m.selectTrue();
        m.selectPreparedTrue();
//        m.updateTrue(); // java.sql.SQLException: Virtuoso 42000 Error SR186:SECURITY: No permission to execute procedure
//        m.updatePreparedTrue(); // java.sql.SQLException: Virtuoso 42000 Error SR186:SECURITY: No permission to execute procedure
    }
}
