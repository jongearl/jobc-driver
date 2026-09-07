package com.frotoma.jobc.rest;

import com.frotoma.jobc.JobcDriverException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractAPIService {

    public boolean isValid(String urlString , int timeout) throws SQLException{
        boolean isValid = false;
        try {
            URL u = new URL(urlString);            
            URLConnection conn = u.openConnection();
            conn.setConnectTimeout(timeout);
            conn.connect();
            isValid = true;
        } catch (MalformedURLException e) {
            throw new SQLException( e );
        } catch (IOException e) {
            throw new SQLException( e );
        }      
        return isValid;
    }


    public abstract String api(String urlString, Map<String, String> param, Properties info) throws JobcDriverException;
    
}
