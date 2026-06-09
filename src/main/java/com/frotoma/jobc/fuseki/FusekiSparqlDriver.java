/*
MIT License

Copyright (c) 2026 jongearl

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
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
