package com.frotoma.jobc.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.config.RequestConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.frotoma.jobc.JobcDriverException;
import com.frotoma.jobc.MockSparqlServer;
import com.frotoma.jobc.fuseki.FusekiSparqlDriver;

public class ApiManagerFixTest {

    private MockSparqlServer server;

    @Before
    public void setUp() throws Exception {
        server = MockSparqlServer.start();
    }

    @After
    public void tearDown() {
        server.close();
    }

    private APIMangerService api(String... kv) {
        Properties p = new Properties();
        for (int i = 0; i < kv.length; i += 2) {
            p.setProperty(kv[i], kv[i + 1]);
        }
        return new APIMangerService(server.base() + "/ds/sparql", p);
    }

    private static Map<String, String> q() {
        Map<String, String> m = new HashMap<>();
        m.put("query", "ASK {}");
        return m;
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(n);
    }

    @Test
    public void defaultsApplied() {
        RequestConfig rc = api().defaultRequestConfig();
        assertEquals(60000, rc.getSocketTimeout());
        assertEquals(5000, rc.getConnectTimeout());
        assertEquals(10000, rc.getConnectionRequestTimeout());
    }

    @Test
    public void invalidNumberFallsBack() {
        assertEquals(60000, api("sockettimeout", "abc").defaultRequestConfig().getSocketTimeout());
    }

    @Test
    public void perRequestTimeout() {
        server.delayMillis(3000);
        APIMangerService a = api();
        long started = System.currentTimeMillis();
        try {
            a.get(server.base() + "/ds/sparql", q(), 300);
            fail("타임아웃이 나야 한다");
        } catch (JobcDriverException expected) {
            long elapsed = System.currentTimeMillis() - started;
            assertTrue("elapsed " + elapsed, elapsed < 2500);
        }
    }

    @Test
    public void acceptHeader() {
        api().get(server.base() + "/ds/sparql", q());
        assertEquals("application/sparql-results+json, application/json;q=0.9", server.lastHeader("Accept"));
        assertNull(server.lastHeader("Content-Type"));
    }

    @Test
    public void status401() {
        server.respond(401, "text/plain", "Unauthorized");
        try {
            api().get(server.base() + "/ds/sparql", q());
            fail();
        } catch (JobcDriverException e) {
            SQLException s = e.toSQLException();
            assertEquals("28000", s.getSQLState());
            assertTrue(s.getMessage(), s.getMessage().startsWith("HTTP 401"));
        }
    }

    @Test
    public void status500BodyTruncated() {
        server.respond(500, "text/plain", repeat('x', 5000));
        try {
            api().post(server.base() + "/ds/sparql", q());
            fail();
        } catch (JobcDriverException e) {
            SQLException s = e.toSQLException();
            assertEquals("08S01", s.getSQLState());
            assertTrue("len " + s.getMessage().length(), s.getMessage().length() <= "HTTP 500: ".length() + 1000);
        }
    }

    @Test
    public void status204ReturnsNull() {
        server.respond(204, null, "");
        assertNull(api().post(server.base() + "/ds/update", q()));
    }

    @Test
    public void oversizedRejected() {
        server.respond(200, "application/json", "{\"a\":\"" + repeat('x', 5000) + "\"}");
        try {
            api("maxresponsebytes", "1000").get(server.base() + "/ds/sparql", q());
            fail();
        } catch (JobcDriverException e) {
            assertTrue(e.toString(), e.toSQLException().getMessage().contains("maxresponsebytes"));
        }
    }

    @Test
    public void oversizedChunkedRejected() {
        server.respond(200, "application/json", "{\"a\":\"" + repeat('x', 5000) + "\"}").chunked(true);
        try {
            api("maxresponsebytes", "1000").get(server.base() + "/ds/sparql", q());
            fail();
        } catch (JobcDriverException e) {
            assertTrue(e.toString(), e.toSQLException().getMessage().contains("maxresponsebytes"));
        }
    }

    @Test
    public void errorsDoNotLeakPool() {
        APIMangerService a = api("maxresponsebytes", "1000", "connectionrequesttimeout", "2000");
        server.respond(200, "application/json", "{\"a\":\"" + repeat('x', 5000) + "\"}").chunked(true);
        for (int i = 0; i < 25; i++) {
            try {
                a.get(server.base() + "/ds/sparql", q());
                fail();
            } catch (JobcDriverException expected) {
                // 크기 초과
            }
        }
        server.respond(500, "text/plain", "boom").chunked(false);
        for (int i = 0; i < 25; i++) {
            try {
                a.post(server.base() + "/ds/sparql", q());
                fail();
            } catch (JobcDriverException expected) {
                // 서버 오류
            }
        }
        server.respond(200, "application/sparql-results+json", MockSparqlServer.DEFAULT_JSON);
        assertEquals(MockSparqlServer.DEFAULT_JSON, a.get(server.base() + "/ds/sparql", q()));
    }

    @Test
    public void fusekiUpdateErrorIsSQLException() throws Exception {
        server.respond(500, "text/plain", "boom");
        Connection c = new FusekiSparqlDriver().connect("jobc:fuseki:" + server.base() + "/ds/sparql", new Properties());
        try {
            c.createStatement().executeUpdate("INSERT DATA { <http://a> <http://b> 1 }");
            fail();
        } catch (SQLException expected) {
            assertEquals("08S01", expected.getSQLState());
        }
    }
}
