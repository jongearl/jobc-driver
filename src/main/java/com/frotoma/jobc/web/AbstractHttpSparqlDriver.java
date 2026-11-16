package com.frotoma.jobc.web;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.frotoma.jobc.JobcDriver;
import com.frotoma.jobc.rest.APIMangerService;

/**
 * HTTP 로 SPARQL 엔드포인트에 붙는 드라이버(Web, Fuseki)의 공통 부분.
 * 접속 URL·속성이 같은 연결끼리 HTTP 연결 풀(APIMangerService)을 나눠 쓰고, 마지막 연결이 닫히면 풀을 닫는다.
 */
public abstract class AbstractHttpSparqlDriver implements java.sql.Driver {

    private final Map<String, APIMangerService> apiServices = new ConcurrentHashMap<>();

    /** 예: "jobc:fuseki:" */
    protected abstract String urlPrefix();

    protected abstract WebSparqlConnection newConnection(String serverUrl, APIMangerService api);

    @Override
    public final Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        String endpoint = url.substring(urlPrefix().length());
        Properties props = urlToInfo(endpoint, info == null ? new Properties() : info);
        String serverUrl = stripParams(endpoint);

        apiServices.values().removeIf(APIMangerService::isClosed);
        APIMangerService api = apiServices.compute(cacheKey(serverUrl, props), (key, existing) -> {
            if (existing != null && existing.tryRetain()) {
                return existing;
            }
            APIMangerService created = new APIMangerService(serverUrl, props);
            created.tryRetain();
            return created;
        });
        return newConnection(serverUrl, api);
    }

    /** 접속 속성(키는 소문자)에 URL 의 "&amp;key=value"(값은 URL 디코딩)를 더한다. URL 쪽 값이 우선한다. */
    static Properties urlToInfo(String endpoint, Properties info) {
        Properties props = new Properties();
        for (String key : info.stringPropertyNames()) {
            props.setProperty(key.toLowerCase(), info.getProperty(key));
        }
        int start = endpoint.indexOf('&');
        if (start > 0) {
            for (String pair : endpoint.substring(start + 1).split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && !kv[0].isEmpty()) {
                    props.setProperty(kv[0].toLowerCase(), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
        }
        return props;
    }

    /** "&amp;" 뒤의 접속 속성을 뗀 서버 URL */
    static String stripParams(String endpoint) {
        int end = endpoint.indexOf('&');
        return end > 0 ? endpoint.substring(0, end) : endpoint;
    }

    /** 풀을 나누는 키. 비밀번호가 문자열로 남지 않도록 속성은 SHA-256 으로 줄인다. */
    static String cacheKey(String serverUrl, Properties props) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<String, String>(toMap(props)).entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return serverUrl + "#" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> toMap(Properties p) {
        Map<String, String> m = new TreeMap<>();
        for (String k : p.stringPropertyNames()) {
            m.put(k, p.getProperty(k));
        }
        return m;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith(urlPrefix());
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return JobcDriver.MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return JobcDriver.MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
        return java.util.logging.Logger.getLogger("com.frotoma.jobc");
    }
}
