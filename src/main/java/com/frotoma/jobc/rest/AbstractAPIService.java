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
