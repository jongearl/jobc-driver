package com.frotoma.jobc.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Properties;

import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.junit.Test;

import com.frotoma.jobc.MockSparqlServer;
import com.frotoma.jobc.fuseki.FusekiSparqlDriver;
import com.frotoma.jobc.web.WebSparqlDriver;

public class PrepareSparqlBuilderFixTest {

    private static final String SELECT = "SELECT ?s WHERE { ?s <http://ex/p> ? }";

    interface Action {
        void run() throws Exception;
    }

    private static Exception thrown(Action a) {
        try {
            a.run();
        } catch (Exception e) {
            return e;
        }
        fail("예외가 나야 한다");
        return null;
    }

    private static String bind(Object value) throws Exception {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder(SELECT);
        b.addParameter(1, value);
        return b.query();
    }

    @Test
    public void injectionStaysOneLiteral() throws Exception {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder("INSERT DATA { <http://ex/s> <http://ex/p> ? }");
        b.addParameter(1, "x\" } ; DROP ALL ; INSERT DATA { <http://a> <http://b> \"y");
        UpdateRequest req = UpdateFactory.create(b.query());
        assertEquals(1, req.getOperations().size());
    }

    @Test
    public void byteBindsTyped() throws Exception {
        String q = bind((byte) 5);
        assertTrue(q, q.contains("\"5\"") && q.contains("XMLSchema#byte"));
    }

    @Test
    public void shortBindsTyped() throws Exception {
        String q = bind((short) 7);
        assertTrue(q, q.contains("\"7\"") && q.contains("XMLSchema#short"));
    }

    @Test
    public void bigDecimalBinds() throws Exception {
        String q = bind(new BigDecimal("1.50"));
        // Jena 는 xsd:decimal 을 SPARQL 축약형(1.50)으로 쓴다. 둘 다 같은 decimal 리터럴이다.
        assertTrue(q, q.contains(" 1.50 ") || (q.contains("\"1.50\"") && q.contains("XMLSchema#decimal")));
    }

    @Test
    public void sqlDateBinds() throws Exception {
        String q = bind(Date.valueOf("2026-11-16"));
        assertTrue(q, q.contains("\"2026-11-16\"") && q.contains("XMLSchema#date>"));
    }

    @Test
    public void timestampBinds() throws Exception {
        String q = bind(Timestamp.valueOf("2026-11-16 10:20:30"));
        assertTrue(q, q.contains("\"2026-11-16T10:20:30\"") && q.contains("XMLSchema#dateTime"));
    }

    @Test
    public void localDateBinds() throws Exception {
        String q = bind(LocalDate.of(2026, 11, 16));
        assertTrue(q, q.contains("\"2026-11-16\"") && q.contains("XMLSchema#date>"));
    }

    @Test
    public void nullRejected() {
        Exception e = thrown(() -> bind(null));
        assertTrue(e.toString(), e instanceof SQLException);
        assertEquals("22004", ((SQLException) e).getSQLState());
    }

    @Test
    public void indexZeroRejected() {
        Exception e = thrown(() -> new PrepareSparqlBuilder(SELECT).addParameter(0, "a"));
        assertTrue(e.toString(), e instanceof SQLException);
        assertEquals("07009", ((SQLException) e).getSQLState());
    }

    @Test
    public void bytesUnsupported() {
        Exception e = thrown(() -> bind(new byte[] { 1 }));
        assertTrue(e.toString(), e instanceof SQLFeatureNotSupportedException);
    }

    @Test
    public void clearParametersRemovesValue() throws Exception {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder(SELECT);
        b.addParameter(1, "abc");
        b.clearParameters();
        assertFalse(b.query(), b.query().contains("abc"));
    }

    @Test
    public void webPreparedSetByteEndToEnd() throws Exception {
        try (MockSparqlServer server = MockSparqlServer.start()) {
            Connection c = new WebSparqlDriver().connect("jobc:web:" + server.base() + "/sparql", new Properties());
            PreparedStatement ps = c.prepareStatement(SELECT);
            ps.setByte(1, (byte) 5);
            assertTrue(ps.executeQuery().next());
        }
    }

    @Test
    public void preparedSetNullThrows() throws Exception {
        Connection c = new FusekiSparqlDriver().connect("jobc:fuseki:http://127.0.0.1:1/ds/sparql", new Properties());
        PreparedStatement ps = c.prepareStatement(SELECT);
        Exception e = thrown(() -> ps.setNull(1, Types.VARCHAR));
        assertTrue(e.toString(), e instanceof SQLException);
        assertEquals("22004", ((SQLException) e).getSQLState());
    }

    @Test
    public void preparedClearParameters() throws Exception {
        try (MockSparqlServer server = MockSparqlServer.start()) {
            Connection c = new FusekiSparqlDriver().connect("jobc:fuseki:" + server.base() + "/ds/sparql&method=post", new Properties());
            PreparedStatement ps = c.prepareStatement(SELECT);
            ps.setString(1, "abc");
            ps.clearParameters();
            ps.setString(1, "def");
            ps.executeQuery();
            assertFalse(server.lastBody(), server.lastBody().contains("abc"));
        }
    }
}
