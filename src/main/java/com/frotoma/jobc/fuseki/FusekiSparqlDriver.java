package com.frotoma.jobc.fuseki;

import com.frotoma.jobc.virt.VirtuosoSparqlDriver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;


public class FusekiSparqlDriver implements java.sql.Driver{
    
    private org.apache.logging.log4j.Logger logger = LogManager.getLogger(FusekiSparqlDriver.class);
    
    public static String DRIVER_FUSEKI_URL_STARTWITH = "jobc:fuseki:";

    private static int MAJOR_VERSION = 1;
    private static int MINOR_VERSION = 1;
    
    private String url = null;

    static {
        try {
          DriverManager.registerDriver(new FusekiSparqlDriver());
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {   
        
        if (!acceptsURL(url)) { return null; }
        
        Properties props = urlToInfo(url, info);        
        return new FusekiSparqlConnection(this.url, props);
    }
    
    protected String extractURL(String urlString){
        String url = urlString;
        if( urlString.startsWith(DRIVER_FUSEKI_URL_STARTWITH)){
            url = urlString.substring(DRIVER_FUSEKI_URL_STARTWITH.length());
        }
        return url;
    }

    protected Properties urlToInfo(String urlString, Properties _info){             
        
        String url = extractURL( urlString );   
        this.url = url;
       
        Properties props = new Properties();        
        Iterator<Object> it = _info.keySet().iterator();
        while(  it.hasNext() ){
            String key = it.next().toString();
            String property = (String)_info.getProperty(key);            
            props.setProperty( key.toLowerCase(), property);
        } 
        // for (Enumeration<String> en = _info.propertyNames(); en.hasMoreElements(); ) {
        //     String key = (String)en.nextElement();
        //     String property = (String)_info.getProperty(key);
        //     props.setProperty(key.toLowerCase(), property);
        // }        
        int endIndex = url.indexOf("&");
        if( endIndex > 0 ){
            String _url = url.substring(0, endIndex);
            String _params = url.substring(endIndex+1);
            this.url = _url;

            String[] keyvalue = _params.split("&");
            for( int i = 0; i < keyvalue.length; i++ ){
                String[] kv = keyvalue[i].split("=");
                if( kv.length ==2 ){
                    props.setProperty(kv[0].toLowerCase(), kv[1]);
                }
            }
        }
        return props;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith(DRIVER_FUSEKI_URL_STARTWITH);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {        
        return null;
    }

    @Override
    public int getMajorVersion() {        
        return MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {        
        return MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {        
        return null;
    }
    
}
