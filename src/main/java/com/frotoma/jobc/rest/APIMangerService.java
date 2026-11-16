package com.frotoma.jobc.rest;

import com.frotoma.jobc.JobcDriverException;
import com.frotoma.jobc.LogMasker;
import com.frotoma.jobc.builder.LogBuilder;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
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
    

    public APIMangerService(String url, Properties info){
        
        this.url = url;
        
        init( info );        
        
        // 1. 커넥션 풀 매니저 설정
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);

        // 2. 타임아웃 설정 (RequestConfig, ms). 접속 속성에 없으면 연결 대기 5초, 나머지는 제한 없음
        RequestConfig.Builder b = RequestConfig.custom()
                .setConnectTimeout(5000);     // 서버 연결 대기 시간
        if ( info.containsKey("connectionrequesttimeout") ) {
            b.setConnectionRequestTimeout( Integer.parseInt( info.getProperty("connectionrequesttimeout") ));
        }
        if( info.containsKey("connecttimeout") ){
            b.setConnectTimeout( Integer.parseInt( info.getProperty("connecttimeout") ) ) ;
        }
        if( info.containsKey("sockettimeout") ){
            b.setSocketTimeout( Integer.parseInt( info.getProperty("sockettimeout") ) );   // 데이터 응답 대기 시간
        }
        RequestConfig requestConfig  = b.build();


        // 3. 빌더에 풀 매니저와 기본 설정을 모두 주입하여 '단 한 번만' 생성
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    private void init(Properties info){

        this.user = info.getProperty("user");

        this.info = info;
        if( url != null && url.toLowerCase().startsWith("http://") && user != null ){
            logger.warn("Basic auth over plain HTTP: credentials are sent unencrypted to {}", LogMasker.maskUrl(url));
        }
    }

    /** 접속 속성. 비밀번호는 밖으로 내주지 않는다. */
    public String getProperty(String key){
        if( "password".equalsIgnoreCase(key) ){
            return null;
        }
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
            }

            HttpResponse httpResponse = this.httpClient.execute(httpGet);
            // System.out.println( "=================== HEADER ====================");
            // for( Header header : httpResponse.getAllHeaders() ){
            //     System.out.println( header.getName() +" : "+ header.getValue() );
            // }
            // System.out.println( "================================================");
            resultJson = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);   // charset 이 없으면 UTF-8 (SPARQL JSON 기본)            
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
        }

        try {
            // 파라미터는 URL 이 아니라 본문(form)으로 보낸다 (긴 SPARQL 의 URL 길이 제한 회피)
            List<NameValuePair> form = new ArrayList<>();
            for (Map.Entry<String, String> entry : param.entrySet()) {
                form.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));


            HttpResponse httpResponse = httpClient.execute(httpPost);
//            logger.debug( "===================  POST Response HEADER ====================");
//            for( Header header : httpResponse.getAllHeaders() ){
//                logger.debug( header.getName() +" : "+ header.getValue() );
//            }
//            logger.debug( "================================================");
            resultJson = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);   // charset 이 없으면 UTF-8 (SPARQL JSON 기본)                        
            int statulsCode = httpResponse.getStatusLine().getStatusCode();            
            if( statulsCode != 200 ){                
                throw new SQLException( resultJson.trim() );
            }
        } catch (ClientProtocolException e) {
            throw new JobcDriverException(e);
        } catch (IOException e) {
            throw new JobcDriverException(e);
        } catch (SQLException ex) {
            throw new JobcDriverException(ex);
        }
        return resultJson;        
    }
    

    /** 서버에 TCP 연결이 되는지만 본다. 연 연결은 바로 닫는다. @param timeoutMillis 연결 대기 ms */
    public boolean isReachable(int timeoutMillis) throws SQLException{
        java.net.URLConnection conn = null;
        try {
            conn = new java.net.URL(url).openConnection();
            conn.setConnectTimeout(timeoutMillis);
            conn.connect();
            return true;
        } catch (IOException e) {
            throw new SQLException( e );
        } finally {
            if( conn instanceof java.net.HttpURLConnection ){
                ((java.net.HttpURLConnection) conn).disconnect();
            }
        }
    }

}
