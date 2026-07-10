/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frotoma.jobc.rest;

import com.frotoma.jobc.JobcDriverException;
import com.frotoma.jobc.builder.LogBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author jongearl
 */
public class APIMangerService {
    
    private static final Logger logger = LoggerFactory.getLogger(APIMangerService.class);
    
    private static final String ACCEPT_SPARQL = "application/sparql-results+json";
    private static final String ACCEPT_JSON = "application/json; charset=utf-8";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";
    
    private final PoolingHttpClientConnectionManager cm;
    
    private final HttpClient httpClient;
    
    private Properties info;
    
    private String url;
    
    private String user;
    
    private String password;

    public APIMangerService(String url, Properties info){
        
        this.url = url;
        
        init( info );        
        
        // 1. 커넥션 풀 매니저 설정
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);

        // 2. 기본 타임아웃 설정 (RequestConfig)
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)     // 서버 연결 대기 시간
                //.setResponseTimeout(10000)   // 데이터 응답 대기 시간
                .build();
        
        RequestConfig.Builder b = RequestConfig.custom();            
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
            

        // 3. 빌더에 풀 매니저와 기본 설정을 모두 주입하여 '단 한 번만' 생성
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(defaultRequestConfig) 
                .build();
    }
    
    private void init(Properties info){
        
        this.user = info.getProperty("user");
        this.password = info.getProperty("password");    
        
        if( !info.contains("connecttimeout") ){
            info.setProperty("connecttimeout", "10000");
        }
        if( !info.contains("sockettimeout") ){
            info.setProperty("sockettimeout", "10000");
        }
        if( !info.contains("connectionrequesttimeout") ){
            info.setProperty("connectionrequesttimeout", "10000");
        }
        this.info = info;
    }
    
    public String getUser(){
        return user;
    }
    
    public String getPassword(){
        return password;
    }
    
    public String getProperty(String key){
        return this.info.getProperty(key);
    }
    
    public void setProperty(String key, String value){
        this.info.setProperty(key, value);
    }

    public String get(String url, Map<String, String> param) throws JobcDriverException{
        
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
            logger.debug( "GET URL : " + _urlString );
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

            HttpResponse httpResponse = this.httpClient.execute(httpGet);
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
    
    public String post(String url, Map<String, String> param) throws JobcDriverException{
        
        // 전송방식 HttpGet, HttpPost방식
        logger.debug( "POST SPARQL : " + LogBuilder.generateOneline( param.get("query") ) );
        logger.debug( "POST URL : " + url );
        logger.debug( "POST PARAM : " + param );
        
        String resultJson = null;
        HttpPost httpPost = new HttpPost( url  );
        
        // 일반 SQL은 select만 가능
        httpPost.setHeader("ACCEPT", ACCEPT_SPARQL);
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
            
            logger.debug( "POST AUTH : " + username +" / PASSWORD " );
        }        

        URI uri = null;
        try {
            URIBuilder ub = new URIBuilder( url );
            Iterator<String> it = param.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                String value = param.get(key);
                ub.addParameter(key,value);                
            }            
            uri = ub.build();
            
            logger.debug( "POST URL : " + uri );

            httpPost.setURI(uri);

            logger.debug( "===================  POST Request HEADER ====================");
            for( Header header : httpPost.getAllHeaders() ){
                logger.debug( header.getName() +" : "+ header.getValue() );
            }
            logger.debug( "================================================");
            

            HttpResponse httpResponse = httpClient.execute(httpPost);
//            logger.debug( "===================  POST Response HEADER ====================");
//            for( Header header : httpResponse.getAllHeaders() ){
//                logger.debug( header.getName() +" : "+ header.getValue() );
//            }
//            logger.debug( "================================================");
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
