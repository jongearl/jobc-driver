package com.frotoma.jobc.rest;

import com.frotoma.jobc.JobcDriverException;
import com.frotoma.jobc.builder.LogBuilder;
import java.io.IOException;
import java.net.URLEncoder;
import org.apache.http.client.config.RequestConfig.Builder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class APISelectGetService extends AbstractAPIService{

    private static final Logger logger = LoggerFactory.getLogger(APISelectGetService.class);

    private static final String ACCEPT_SPARQL = "application/sparql-results+json";
    private static final String ACCEPT_JSON = "application/json; charset=utf-8";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

    public String api(String url, Map<String, String> param, Properties info) throws JobcDriverException{
        
        String resultJson = null;        
        try {
            int idx = 0;
            
            StringBuilder urlBuffer = new StringBuilder( url );
            for (String key : param.keySet()) {
                String value = param.get(key);
                urlBuffer.append((idx == 0) ? "?" : "&");
                urlBuffer.append(key).append("=").append(URLEncoder.encode(value, "UTF-8"));
                idx++;
            }

            String _urlString = urlBuffer.toString();
            
            logger.debug( "GET SPARQL : " + LogBuilder.generateOneline( param.get("query") ) );
            logger.debug( "GET URL : " + url );
            logger.debug( "GET PARAM : " + param );
            
            HttpGet httpGet = new HttpGet(_urlString);            
            httpGet.setHeader("ACCEPT", ACCEPT_SPARQL);
            httpGet.setHeader("ACCEPT", ACCEPT_JSON);        
            httpGet.setHeader("Content-Type", CONTENT_TYPE);

            String username = info.getProperty("user");
            String password = info.getProperty("password");
            if (username != null && password != null) {
                String auth = username + ":" + password;
                byte[] data1 = auth.getBytes(StandardCharsets.UTF_8);
                String base64 = Base64.getEncoder().encodeToString(data1);                
                httpGet.setHeader("Authorization", "Basic " + base64);
                
                logger.debug( "GET AUTH : " + username +" / PASSWORD " );
            }

            
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

            httpGet.setConfig(requestConfig);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse httpResponse = httpClient.execute(httpGet);
            // System.out.println( "=================== HEADER ====================");
            // for( Header header : httpResponse.getAllHeaders() ){
            //     System.out.println( header.getName() +" : "+ header.getValue() );
            // }
            // System.out.println( "================================================");
            resultJson = EntityUtils.toString(httpResponse.getEntity());            
            int statulsCode = httpResponse.getStatusLine().getStatusCode();
            if( statulsCode != 200 ){
                throw new SQLException( resultJson.trim() );
            }
        } catch (IOException ex) {
            throw new JobcDriverException(ex);
        } catch (SQLException ex) {
            throw new JobcDriverException(ex);
        }
        return resultJson;
    }
    
}
