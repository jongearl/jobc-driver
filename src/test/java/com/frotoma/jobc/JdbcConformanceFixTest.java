package com.frotoma.jobc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.frotoma.jobc.fuseki.FusekiSparqlDriver;
import com.frotoma.jobc.web.WebSparqlConnection;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class JdbcConformanceFixTest {

    private static final String INSERT = "INSERT DATA { <http://a> <http://b> 1 }";

    private MockSparqlServer server;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        server = MockSparqlServer.start();
        conn = new FusekiSparqlDriver().connect("jobc:fuseki:" + server.base() + "/ds/sparql", new Properties());
    }

    @After
    public void tearDown() throws Exception {
        conn.close();
        server.close();
    }

    @Test
    public void fusekiTypedOverloadUsesUpdateEndpoint() throws Exception {
        conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeUpdate(INSERT);
        assertEquals("/ds/update", server.lastPath());
        assertTrue(server.lastBody(), server.lastBody().startsWith("update="));
    }

    @Test
    public void threeArgPrepareNotNull() throws Exception {
        assertNotNull(conn.prepareStatement("ASK {}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT));
        assertNotNull(conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT));
    }

    @Test
    public void prepareCallUnsupported() {
        try {
            conn.prepareCall("x");
            fail();
        } catch (SQLException e) {
            assertTrue(e.toString(), e instanceof SQLFeatureNotSupportedException);
        }
    }

    @Test
    public void getMetaDataUnsupported() {
        try {
            conn.getMetaData();
            fail();
        } catch (SQLException e) {
            assertTrue(e.toString(), e instanceof SQLFeatureNotSupportedException);
        }
    }

    @Test
    public void networkTimeoutRoundTrip() throws Exception {
        conn.setNetworkTimeout(null, 1234);
        assertEquals(1234, conn.getNetworkTimeout());
    }

    @Test
    public void queryTimeoutApplied() throws Exception {
        server.delayMillis(3000);
        Statement st = conn.createStatement();
        st.setQueryTimeout(1);
        assertEquals(1, st.getQueryTimeout());
        long started = System.currentTimeMillis();
        try {
            st.executeQuery("ASK {}");
            fail();
        } catch (SQLException expected) {
            long elapsed = System.currentTimeMillis() - started;
            assertTrue("elapsed " + elapsed, elapsed < 2500);
        }
    }

    @Test
    public void batchQueuesUntilExecute() throws Exception {
        Statement st = conn.createStatement();
        st.addBatch(INSERT);
        st.addBatch(INSERT);
        assertEquals(0, server.requestCount());
        assertEquals(2, st.executeBatch().length);
        assertEquals(2, server.requestCount());
    }

    @Test
    public void preparedBatchQueuesUntilExecute() throws Exception {
        PreparedStatement ps = conn.prepareStatement("INSERT DATA { <http://a> <http://b> ? }");
        ps.setInt(1, 1);
        ps.addBatch();
        ps.setInt(1, 2);
        ps.addBatch();
        assertEquals(0, server.requestCount());
        assertEquals(2, ps.executeBatch().length);
        assertEquals(2, server.requestCount());
    }

    @Test
    public void batchFailure() throws Exception {
        server.respond(500, "text/plain", "boom");
        Statement st = conn.createStatement();
        st.addBatch(INSERT);
        st.addBatch(INSERT);
        try {
            st.executeBatch();
            fail();
        } catch (BatchUpdateException e) {
            assertEquals(0, e.getUpdateCounts().length);
        }
        assertEquals(0, st.executeBatch().length);
    }

    @Test
    public void unwrapSelf() throws Exception {
        assertSame(conn, conn.unwrap(WebSparqlConnection.class));
        assertTrue(conn.isWrapperFor(Connection.class));
    }

    @Test
    public void closedStatementRejectsWork() throws Exception {
        Statement st = conn.createStatement();
        st.close();
        assertTrue(st.isClosed());
        try {
            st.executeQuery("ASK {}");
            fail();
        } catch (SQLException expected) {
            // 닫힌 Statement
        }
    }

    @Test
    public void hikariPoolSmoke() throws Exception {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jobc:fuseki:" + server.base() + "/ds/sparql");
        cfg.setDriverClassName("com.frotoma.jobc.JobcDriver");
        cfg.setMaximumPoolSize(2);
        cfg.setMinimumIdle(1);
        try (HikariDataSource ds = new HikariDataSource(cfg);
             Connection c = ds.getConnection()) {
            assertTrue(c.createStatement().executeQuery("ASK {}") != null);
        }
    }
}
