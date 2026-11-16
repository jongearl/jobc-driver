package com.frotoma.jobc.rest;

import com.frotoma.jobc.JobcDriverException;
import com.frotoma.jobc.LogMasker;
import com.frotoma.jobc.builder.LogBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 접속(URL + 속성)마다 하나씩 두는 HTTP 클라이언트. 연결 풀과 타임아웃, 응답 크기 제한을 관리한다.
 * @author jongearl
 */
public class APIMangerService implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(APIMangerService.class);

    private static final String ACCEPT = "application/sparql-results+json, application/json;q=0.9";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

    static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 10000;
    static final int DEFAULT_SOCKET_TIMEOUT = 60000;
    static final long DEFAULT_MAX_RESPONSE_BYTES = 64L * 1024 * 1024;

    /** 오류 응답 본문 중 예외 메시지에 넣는 최대 글자 수 */
    private static final int MAX_ERROR_BODY = 1000;

    private final PoolingHttpClientConnectionManager cm;

    private final CloseableHttpClient httpClient;

    private final RequestConfig defaultConfig;

    private final long maxResponseBytes;

    private final Properties info;

    private final String url;

    public APIMangerService(String url, Properties info){

        this.url = url;
        this.info = info;

        if( url != null && url.toLowerCase().startsWith("http://") && info.getProperty("user") != null ){
            logger.warn("Basic auth over plain HTTP: credentials are sent unencrypted to {}", LogMasker.maskUrl(url));
        }

        // 1. 커넥션 풀 매니저 설정
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);

        // 2. 타임아웃 설정 (ms). 접속 속성에 없거나 숫자가 아니면 기본값
        this.defaultConfig = RequestConfig.custom()
                .setConnectTimeout( intProp(info, "connecttimeout", DEFAULT_CONNECT_TIMEOUT) )
                .setConnectionRequestTimeout( intProp(info, "connectionrequesttimeout", DEFAULT_CONNECTION_REQUEST_TIMEOUT) )
                .setSocketTimeout( intProp(info, "sockettimeout", DEFAULT_SOCKET_TIMEOUT) )
                .build();
        this.maxResponseBytes = longProp(info, "maxresponsebytes", DEFAULT_MAX_RESPONSE_BYTES);

        // 3. 빌더에 풀 매니저와 기본 설정을 모두 주입하여 '단 한 번만' 생성
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(defaultConfig)
                // 리다이렉트를 따라가면 손으로 넣은 Authorization 헤더가 다른 호스트로 넘어간다
                .disableRedirectHandling()
                .build();
    }

    private static int intProp(Properties p, String key, int def){
        return (int) longProp(p, key, def);
    }

    private static long longProp(Properties p, String key, long def){
        String v = p.getProperty(key);
        if( v == null ){
            return def;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid value for '{}': '{}'. Using default {}", key, v, def);
            return def;
        }
    }

    RequestConfig defaultRequestConfig(){
        return defaultConfig;
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
        return get(url, param, 0);
    }

    /**
     * @param timeoutMillis 이 요청의 응답 대기 시간(ms). 0 이하이면 접속 기본값
     * @return 응답 본문. 본문이 없으면(204 등) null
     */
    public String get(String url, Map<String, String> param, int timeoutMillis) throws JobcDriverException{
        StringBuilder urlBuffer = new StringBuilder( url );
        int idx = 0;
        for (Map.Entry<String, String> entry : param.entrySet()) {
            urlBuffer.append((idx++ == 0) ? "?" : "&");
            urlBuffer.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        logger.debug( "GET SPARQL : {}", LogBuilder.generateOneline( param.get("query") ) );
        logger.debug( "GET URL : {}", LogMasker.maskUrl(url) );

        return execute(new HttpGet(urlBuffer.toString()), timeoutMillis);
    }

    public String post(String url, Map<String, String> param) throws JobcDriverException{
        return post(url, param, 0);
    }

    /**
     * 파라미터는 URL 이 아니라 본문(form)으로 보낸다 (긴 SPARQL 의 URL 길이 제한 회피)
     * @param timeoutMillis 이 요청의 응답 대기 시간(ms). 0 이하이면 접속 기본값
     * @return 응답 본문. 본문이 없으면(204 등) null
     */
    public String post(String url, Map<String, String> param, int timeoutMillis) throws JobcDriverException{
        logger.debug( "POST SPARQL : {}", LogBuilder.generateOneline( param.containsKey("query") ? param.get("query") : param.get("update") ) );
        logger.debug( "POST URL : {}", LogMasker.maskUrl(url) );

        HttpPost httpPost = new HttpPost( url );
        httpPost.setHeader("Content-Type", CONTENT_TYPE);
        List<NameValuePair> form = new ArrayList<>();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            form.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8));
        return execute(httpPost, timeoutMillis);
    }

    private String execute(HttpRequestBase request, int timeoutMillis) throws JobcDriverException{
        request.setHeader("Accept", ACCEPT);
        String username = info.getProperty("user");
        String password = info.getProperty("password");
        if( username != null && password != null ){
            String auth = username + ":" + password;
            request.setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)));
        }
        if( timeoutMillis > 0 ){
            request.setConfig(RequestConfig.copy(defaultConfig).setSocketTimeout(timeoutMillis).build());
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            String body = readBounded(request, response.getEntity(), maxResponseBytes);
            if( status < 200 || status >= 300 ){
                String text = body == null ? "" : body.trim();
                if( text.length() > MAX_ERROR_BODY ){
                    text = text.substring(0, MAX_ERROR_BODY);
                }
                throw new JobcDriverException(new SQLException("HTTP " + status + ": " + text, sqlState(status), status));
            }
            return ( body == null || body.isEmpty() ) ? null : body;
        } catch (IOException e) {
            request.abort();
            throw new JobcDriverException(new SQLException(e.getMessage(), "08S01", e));
        }
    }

    private static String sqlState(int status){
        if( status == 401 || status == 403 ) return "28000";
        if( status == 404 ) return "08001";
        if( status >= 500 ) return "08S01";
        return "HY000";
    }

    /** 응답 본문을 최대 maxBytes 까지만 읽는다. 넘으면 요청을 끊고(연결은 풀에 돌려주지 않음) 예외. */
    private static String readBounded(HttpRequestBase request, HttpEntity entity, long maxBytes) throws IOException{
        if( entity == null ){
            return null;
        }
        String tooLarge = "Response exceeds maxresponsebytes=" + maxBytes;
        if( entity.getContentLength() > maxBytes ){
            request.abort();
            throw new IOException(tooLarge);
        }
        try (InputStream in = entity.getContent()) {
            if( in == null ){
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while( (n = in.read(buf)) != -1 ){
                total += n;
                if( total > maxBytes ){
                    request.abort();
                    throw new IOException(tooLarge);
                }
                out.write(buf, 0, n);
            }
            // charset 이 없으면 UTF-8 (SPARQL JSON 기본)
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    /** 서버에 TCP 연결이 되는지만 본다. 연 연결은 바로 닫는다. @param timeoutMillis 연결 대기 ms */
    public boolean isReachable(int timeoutMillis) throws SQLException{
        java.net.URLConnection conn = null;
        try {
            conn = new java.net.URL(url).openConnection();
            conn.setConnectTimeout(timeoutMillis);
            conn.setReadTimeout(timeoutMillis);
            conn.connect();
            return true;
        } catch (java.net.MalformedURLException e) {
            throw new SQLException( e );
        } catch (IOException e) {
            // JDBC isValid: 접속할 수 없으면 false
            logger.debug( "Endpoint not reachable: {}", e.toString() );
            return false;
        } finally {
            if( conn instanceof java.net.HttpURLConnection ){
                ((java.net.HttpURLConnection) conn).disconnect();
            }
        }
    }

    private int references = 0;

    private boolean closed = false;

    /** 이 풀을 쓰는 연결이 하나 늘었다. 이미 닫힌 풀이면 false (새로 만들어야 한다) */
    public synchronized boolean tryRetain(){
        if( closed ){
            return false;
        }
        references++;
        return true;
    }

    /** 이 풀을 쓰는 연결이 하나 줄었다. 0이 되면 풀을 닫는다. */
    public synchronized void release(){
        if( references > 0 && --references == 0 ){
            close();
        }
    }

    public synchronized boolean isClosed(){
        return closed;
    }

    @Override
    public synchronized void close(){
        if( !closed ){
            closed = true;
            cm.close();
        }
    }

}
