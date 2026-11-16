package com.frotoma.jobc.rest;

import com.frotoma.jobc.JobcDriverException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractAPIService {

    /** 서버에 TCP 연결이 되는지만 본다. 연 연결은 바로 닫는다. @param timeout 연결 대기 ms */
    public boolean isValid(String urlString , int timeout) throws SQLException{
        boolean isValid = false;
        URLConnection conn = null;
        try {
            URL u = new URL(urlString);
            conn = u.openConnection();
            conn.setConnectTimeout(timeout);
            conn.connect();
            isValid = true;
        } catch (MalformedURLException e) {
            throw new SQLException( e );
        } catch (IOException e) {
            throw new SQLException( e );
        } finally {
            if( conn instanceof HttpURLConnection ){
                ((HttpURLConnection) conn).disconnect();
            }
        }
        return isValid;
    }


    public abstract String api(String urlString, Map<String, String> param, Properties info) throws JobcDriverException;
    
}
