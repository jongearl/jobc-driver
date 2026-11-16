package com.frotoma.jobc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.frotoma.jobc.fuseki.FusekiSparqlDriver;
import com.frotoma.jobc.obj.LiteralObj;
import com.frotoma.jobc.web.WebSparqlConnection;
import com.sun.net.httpserver.HttpServer;

/**
 * 외부 서버 없이 도는 드라이버 테스트: 리터럴 변환, 접속별 설정 분리, URL 속성, POST 본문, 응답 타임아웃.
 */
public class JobcDriverFixTest {

    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";

    private static final String RESULT_JSON = "{\"head\":{\"vars\":[\"n\"]},\"results\":{\"bindings\":["
            + "{\"n\":{\"type\":\"literal\",\"value\":\"007\",\"datatype\":\"" + XSD + "integer\"}}]}}";

    private HttpServer server;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> rawQuery = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();
    private volatile long delayMillis = 0;
    private volatile String responseJson = RESULT_JSON;
    private volatile String contentType = "application/sparql-results+json";

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getPath());
            rawQuery.set(exchange.getRequestURI().getRawQuery());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] out = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            } catch (IOException ignored) {
                // 클라이언트가 타임아웃으로 먼저 끊은 경우
            }
        });
        server.start();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    private String base() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    // ---- 리터럴 ----

    @Test
    public void literalKeepsLexicalFormAndConvertedValue() {
        LiteralObj l = new LiteralObj("007", null, XSD + "integer");
        assertEquals(7, l.getValue());
        assertEquals("007", l.getLexicalForm());
    }

    @Test
    public void literalThatCannotBeConvertedStaysString() {
        LiteralObj l = new LiteralObj("abc", null, XSD + "integer");
        assertEquals("abc", l.getValue());
        assertEquals("abc", l.getLexicalForm());
    }

    @Test
    public void integerBeyondIntRangeBecomesBigInteger() {
        LiteralObj l = new LiteralObj("12345678901234567890", null, "xsd:integer");
        assertEquals(new BigInteger("12345678901234567890"), l.getValue());
    }

    @Test
    public void plainLiteralUnchanged() {
        LiteralObj l = new LiteralObj("화성시", "ko", null);
        assertEquals("화성시", l.getValue());
        assertEquals("화성시", l.getLexicalForm());
        assertEquals("ko", l.getLang());
    }

    // ---- 접속 ----

    @Test
    public void connectionsWithDifferentUrlsDoNotShareSettings() throws SQLException {
        FusekiSparqlDriver driver = new FusekiSparqlDriver();
        WebSparqlConnection a = (WebSparqlConnection) driver.connect("jobc:fuseki:http://a.example/A/sparql", new Properties());
        WebSparqlConnection b = (WebSparqlConnection) driver.connect("jobc:fuseki:http://b.example/B/sparql", new Properties());
        WebSparqlConnection a2 = (WebSparqlConnection) driver.connect("jobc:fuseki:http://a.example/A/sparql", new Properties());

        assertEquals("http://a.example/A/sparql", a.getUrl());
        assertEquals("http://b.example/B/sparql", b.getUrl());
        assertNotSame(a.getAPIManger(), b.getAPIManger());
        assertSame(a.getAPIManger(), a2.getAPIManger());
    }

    @Test
    public void differentCredentialsGetDifferentSettings() throws SQLException {
        FusekiSparqlDriver driver = new FusekiSparqlDriver();
        Properties p1 = new Properties();
        p1.setProperty("user", "u1");
        p1.setProperty("password", "p1");
        Properties p2 = new Properties();
        p2.setProperty("user", "u1");
        p2.setProperty("password", "p2");
        WebSparqlConnection a = (WebSparqlConnection) driver.connect("jobc:fuseki:http://a.example/A/sparql", p1);
        WebSparqlConnection b = (WebSparqlConnection) driver.connect("jobc:fuseki:http://a.example/A/sparql", p2);

        assertNotSame(a.getAPIManger(), b.getAPIManger());
    }

    @Test
    public void urlParamsBecomeConnectionProperties() throws SQLException {
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver()
                .connect("jobc:fuseki:http://a.example/A/sparql&method=post&socketTimeout=500", new Properties());

        assertEquals("http://a.example/A/sparql", c.getUrl());
        assertEquals("post", c.getValue("method"));
        assertEquals("500", c.getValue("sockettimeout"));
    }

    // ---- HTTP ----

    @Test
    public void postSendsQueryInBodyAndKeepsUrlCase() throws SQLException {
        String sparql = "SELECT ?n WHERE { ?s <http://ex/p> ?n }";
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver()
                .connect("jobc:fuseki:" + base() + "/MyDataset/sparql&method=post", new Properties());

        ResultSet rs = c.createStatement().executeQuery(sparql);

        assertEquals("POST", method.get());
        assertEquals("/MyDataset/sparql", path.get());
        assertEquals(null, rawQuery.get());
        assertEquals(sparql, URLDecoder.decode(body.get().substring("query=".length()), StandardCharsets.UTF_8));
        assertTrue(rs.next());
        assertEquals("007", ((LiteralObj) rs.getObject("n")).getLexicalForm());
    }

    /** 연결 검사(Hikari 가 커넥션을 꺼낼 때 부름)가 연 소켓을 닫는다 */
    @Test
    public void isValidClosesItsSocket() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver().connect(
                    "jobc:fuseki:http://127.0.0.1:" + socket.getLocalPort() + "/ds/sparql", new Properties());

            assertTrue(c.isValid(1));

            socket.setSoTimeout(2000);
            try (Socket accepted = socket.accept()) {
                accepted.setSoTimeout(2000);
                assertEquals("클라이언트가 소켓을 닫아야 한다", -1, accepted.getInputStream().read());
            }
        }
    }

    @Test
    public void responseWithoutCharsetIsReadAsUtf8() throws SQLException {
        responseJson = "{\"head\":{\"vars\":[\"n\"]},\"results\":{\"bindings\":["
                + "{\"n\":{\"type\":\"literal\",\"value\":\"동탄\",\"xml:lang\":\"ko\"}}]}}";
        contentType = "application/sparql-results+json";
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver()
                .connect("jobc:fuseki:" + base() + "/ds/sparql&method=post", new Properties());

        ResultSet rs = c.createStatement().executeQuery("SELECT ?n WHERE { ?s ?p ?n }");

        assertTrue(rs.next());
        assertEquals("동탄", ((LiteralObj) rs.getObject("n")).getLexicalForm());
    }

    @Test
    public void socketTimeoutIsApplied() throws SQLException {
        delayMillis = 3000;
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver()
                .connect("jobc:fuseki:" + base() + "/ds/sparql&sockettimeout=300", new Properties());

        long started = System.currentTimeMillis();
        try {
            c.createStatement().executeQuery("ASK {}");
            fail("타임아웃이 나야 한다");
        } catch (SQLException expected) {
            long elapsed = System.currentTimeMillis() - started;
            assertTrue("elapsed " + elapsed, elapsed < 2500);
        }
    }

}
