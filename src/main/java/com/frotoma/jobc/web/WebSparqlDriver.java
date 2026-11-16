package com.frotoma.jobc.web;

import com.frotoma.jobc.rest.APIMangerService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class WebSparqlDriver implements java.sql.Driver{
    
    public static final String DRIVER_WEB_URL_STARTWITH = "jobc:web:";

    private static int MAJOR_VERSION = 1;
    private static int MINOR_VERSION = 1;

    // DriverManager 에는 드라이버 인스턴스가 하나만 등록되므로 접속 URL·속성별로 따로 둔다.
    private final Map<String, APIMangerService> apiMangeerServices = new ConcurrentHashMap<>();

    static {
        try {
          DriverManager.registerDriver(new WebSparqlDriver());
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {    
        if (!acceptsURL(url)) {
            return null;
        }
        
        String endpoint = extractURL(url);
        Properties props = urlToInfo(endpoint, info);
        String serverUrl = stripParams(endpoint);
        APIMangerService apiMangeerService = apiMangeerServices.computeIfAbsent(
                serverUrl + " " + new TreeMap<>(props), key -> new APIMangerService(serverUrl, props));
        return new WebSparqlConnection(serverUrl, apiMangeerService);
    }

    /** "&amp;" 뒤의 접속 속성을 뗀 서버 URL */
    protected String stripParams(String endpoint){
        int endIndex = endpoint.indexOf("&");
        return endIndex > 0 ? endpoint.substring(0, endIndex) : endpoint;
    }
    
    protected String extractURL(String urlString){        
        String url = urlString;
        if( urlString.startsWith(DRIVER_WEB_URL_STARTWITH)){            
            url = urlString.substring(DRIVER_WEB_URL_STARTWITH.length());            
        }
        return url;
    }

    /** 접속 속성(키는 소문자)에 URL 의 "&amp;key=value" 를 더한다. URL 쪽 값이 우선한다. */
    protected Properties urlToInfo(String url, Properties _info){
        
        Properties props = new Properties();        
        Iterator<Object> it = _info.keySet().iterator();
        while(  it.hasNext() ){
            String key = it.next().toString();
            String property = (String)_info.getProperty(key);
            props.setProperty(key.toLowerCase(), property);
        } 
        // for (Enumeration<String> en = _info.propertyNames(); en.hasMoreElements(); ) {
        //     String key = (String)en.nextElement();
        //     String property = (String)_info.getProperty(key);
        //     props.setProperty(key.toLowerCase(), property);
        // }        
        int endIndex = url.indexOf("&");
        if( endIndex > 0 ){
            String _params = url.substring(endIndex+1);

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
        return url.startsWith(DRIVER_WEB_URL_STARTWITH);
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
