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
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.config.RequestConfig.Builder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * update용
 */
public class APIUpdatePostFusekiService extends APISelectPostService{

    private static final Logger logger = LoggerFactory.getLogger(APIUpdatePostFusekiService.class);

    private static final String ACCEPT_SPARQL_UPDATE = "application/sparql-update; charset=utf-8";
    private static final String ACCEPT_SPARQL = "application/sparql-results+json; charset=utf-8";
    private static final String ACCEPT_JSON = "application/json; charset=utf-8";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

    public String api(String urlString, Map<String, String> param, Properties info) throws JobcDriverException{
        
        logger.info( "POST UPDATE SPARQL : " + param.get("update"));
        // 전송방식 HttpGet, HttpPost방식
        String resultJson = null;
        HttpPost httpPost = new HttpPost( urlString );
        
        // 일반 SQL은 select만 가능
        httpPost.setHeader("ACCEPT", ACCEPT_SPARQL_UPDATE);
        httpPost.setHeader("ACCEPT", ACCEPT_JSON);        
        httpPost.setHeader("Content-Type", CONTENT_TYPE);
        

        String username = info.getProperty("user");
        String password = info.getProperty("password");
        String base64 = null;
        if( username != null && password != null ){
            String auth = username + ":" + password;
            byte[] data1 = auth.getBytes(StandardCharsets.UTF_8);
            base64 = Base64.getEncoder().encodeToString(data1);
            httpPost.addHeader("Authorization", "Basic " + base64);
        }
        

        URI uri = null;
        try {
            URIBuilder ub = new URIBuilder( urlString );
            Iterator<String> it = param.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                String value = param.get(key);
                ub.addParameter(key,value);                
            }            
            uri = ub.build();

            logger.info( "POST : "+ uri );

            httpPost.setURI(uri);

            Builder b = RequestConfig.custom();            
            if ( info.containsKey("connectionrequesttimeout") ) {                
                b.setConnectionRequestTimeout( Integer.parseInt( info.getProperty("connectionrequesttimeout", "10000") ));
            }            
            if( info.containsKey("connecttimeout") ){
                b.setConnectTimeout( Integer.parseInt( info.getProperty("connecttimeout", "10000") ) ) ;
            }            
            if( info.containsKey("sockettimeout") ){
                b.setSocketTimeout( Integer.parseInt( info.getProperty("sockettimeout", "10000") ) );
            }
            RequestConfig requestConfig  = b.build();
            httpPost.setConfig(requestConfig);

            logger.debug( "===================  POST Request HEADER ====================");
            for( Header header : httpPost.getAllHeaders() ){
                logger.debug( header.getName() +" : "+ header.getValue() );
            }
            logger.debug( "================================================");
            

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            
            resultJson = EntityUtils.toString(httpResponse.getEntity());              
            
            int statulsCode = httpResponse.getStatusLine().getStatusCode();            
            if( statulsCode != 200 ){                
                throw new SQLException( resultJson.trim() );
            }
            
        } catch (URISyntaxException e) {   
            throw new JobcDriverException(e);
        } catch (ClientProtocolException e) {
            throw new JobcDriverException(e);
        } catch (IOException e) {
            throw new JobcDriverException(e);
        } catch (SQLException ex) {
            throw new JobcDriverException(ex);
        }
        return resultJson;        
    }

}
