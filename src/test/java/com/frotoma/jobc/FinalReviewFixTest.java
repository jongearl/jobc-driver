package com.frotoma.jobc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.frotoma.jobc.builder.PrepareSparqlBuilder;
import com.frotoma.jobc.fuseki.FusekiSparqlDriver;
import com.frotoma.jobc.obj.LiteralObj;

/**
 * 최종 리뷰에서 나온 Critical/Important 항목 재현 테스트.
 */
public class FinalReviewFixTest {

    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";

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

    interface Action {
        void run() throws Exception;
    }

    private static SQLException sqlError(Action a) {
        try {
            a.run();
        } catch (SQLException e) {
            return e;
        } catch (Exception e) {
            fail("SQLException 이어야 한다: " + e);
        }
        fail("예외가 나야 한다");
        return null;
    }

    private static String lit(String var, String value, String type) {
        return "\"" + var + "\":{\"type\":\"literal\",\"value\":\"" + value + "\",\"datatype\":\"" + XSD + type + "\"}";
    }

    private ResultSet row(String vars, String bindings) throws SQLException {
        server.respond(200, "application/sparql-results+json",
                "{\"head\":{\"vars\":[" + vars + "]},\"results\":{\"bindings\":[{" + bindings + "}]}}");
        ResultSet rs = conn.createStatement().executeQuery("SELECT * {}");
        assertTrue(rs.next());
        return rs;
    }

    // ---- Critical 1: LiteralObj lang/datatype 인젝션 ----

    @Test
    public void literalObjBadLangRejected() {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder("SELECT * { ?s ?p ? }");
        sqlError(() -> b.addParameter(1, new LiteralObj("1", "en } DROP ALL #", null)));
    }

    @Test
    public void literalObjBadDatatypeRejected() {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder("SELECT * { ?s ?p ? }");
        sqlError(() -> b.addParameter(1, new LiteralObj("1", null, "http://x> } ; DROP ALL ; #")));
    }

    @Test
    public void literalObjValidStillBinds() throws Exception {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder("SELECT * { ?s ?p ? }");
        b.addParameter(1, new LiteralObj("hi", "en-US", null));
        assertTrue(b.query(), b.query().contains("\"hi\"@en-US"));
        b.addParameter(1, new LiteralObj("5", null, "xsd:int"));
        assertTrue(b.query(), b.query().contains("XMLSchema#int"));
    }

    // ---- Important 1: 리다이렉트로 인증정보 전달 ----

    @Test
    public void credentialsNotForwardedOnRedirect() throws Exception {
        try (MockSparqlServer other = MockSparqlServer.start()) {
            server.respond(302, "text/plain", "moved").header("Location", other.base() + "/x");
            Properties p = new Properties();
            p.setProperty("user", "u");
            p.setProperty("password", "topsecret");
            Connection c = new FusekiSparqlDriver().connect("jobc:fuseki:" + server.base() + "/r/sparql", p);
            sqlError(() -> c.createStatement().executeQuery("ASK {}"));
            assertEquals(0, other.requestCount());
        }
    }

    // ---- Important 2: URL 속성의 잘못된 % ----

    @Test
    public void rawPercentInUrlPasswordIsSQLExceptionWithoutSecret() {
        SQLException e = sqlError(() -> new FusekiSparqlDriver()
                .connect("jobc:fuseki:http://h/ds/sparql&user=u&password=50%off", new Properties()));
        assertEquals("08001", e.getSQLState());
        assertFalse(e.getMessage(), e.getMessage().contains("of"));
        assertNull(e.getCause());
    }

    // ---- Important 3: LogMasker ----

    @Test
    public void maskerCoversSemicolonQuestionAt() {
        assertEquals("http://h/ds&password=****&m=1", LogMasker.maskUrl("http://h/ds&password=ab;cd?x&m=1"));
        assertEquals("jdbc:virtuoso://h:1111/UID=dba/PWD=****/charset=UTF-8",
                LogMasker.maskUrl("jdbc:virtuoso://h:1111/UID=dba/PWD=s3c;ret/charset=UTF-8"));
        assertEquals("http://u:****@h/ds", LogMasker.maskUrl("http://u:p@ss@h/ds"));
    }

    // ---- Important 4: isValid ----

    @Test
    public void isValidFalseWhenServerDown() throws Exception {
        int port;
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = s.getLocalPort();
        }
        Connection c = new FusekiSparqlDriver().connect("jobc:fuseki:http://127.0.0.1:" + port + "/ds/sparql", new Properties());
        assertFalse(c.isValid(1));
    }

    @Test
    public void isValidNegativeTimeoutThrows() {
        sqlError(() -> conn.isValid(-1));
    }

    // ---- Important 5: ')' '}' ',' 앞의 '?' ----

    @Test
    public void placeholderBeforeParenIsBound() throws Exception {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder("SELECT * { ?s <http://p> ?o FILTER(?o = ?) ?s <http://q> ? }");
        b.addParameter(1, "first");
        b.addParameter(2, "second");
        String q = b.query();
        assertTrue(q, q.contains("FILTER(?o = \"first\" )") || q.contains("FILTER(?o = \"first\")"));
        assertTrue(q, q.contains("<http://q> \"second\""));
    }

    @Test
    public void placeholderBeforeBraceAndComma() throws Exception {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder("SELECT * { ?s ?p ?} VALUES ?x { ? } FILTER(?s IN (?,?))");
        b.addParameter(1, "a");
        b.addParameter(2, "b");
        b.addParameter(3, "c");
        b.addParameter(4, "d");
        String q = b.query();
        assertTrue(q, q.contains("\"a\"") && q.contains("\"b\"") && q.contains("\"c\"") && q.contains("\"d\""));
    }

    @Test
    public void questionMarkInsideStringOrIriIsNotPlaceholder() throws Exception {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder("SELECT * { ?s <http://ex/a?b=1> \"what?\" . ?s ?p ? FILTER(?o < ?) }");
        b.addParameter(1, "x");
        b.addParameter(2, 3);
        String q = b.query();
        assertTrue(q, q.contains("<http://ex/a?b=1>") && q.contains("\"what?\"") && q.contains("\"x\""));
    }

    @Test
    public void unboundPlaceholderThrows() {
        PrepareSparqlBuilder b = new PrepareSparqlBuilder("SELECT * { ?s ?p ? }");
        SQLException e = sqlError(b::query);
        assertEquals("07001", e.getSQLState());
    }

    // ---- Important 6: ResultSet 값 ----

    @Test
    public void unboundDateTimestampBytesAreNull() throws Exception {
        ResultSet rs = row("\"m\"", "");
        assertNull(rs.getDate("m"));
        assertTrue(rs.wasNull());
        assertNull(rs.getTimestamp("m"));
        assertNull(rs.getTime("m"));
        assertNull(rs.getBytes("m"));
    }

    @Test
    public void timestampFromXsdDateTime() throws Exception {
        ResultSet rs = row("\"t\"", lit("t", "2026-11-16T10:20:30", "dateTime"));
        assertEquals(Timestamp.valueOf("2026-11-16 10:20:30"), rs.getTimestamp("t"));
        assertEquals(java.sql.Date.valueOf("2026-11-16"), rs.getDate("t"));
    }

    @Test
    public void getObjectWithClass() throws Exception {
        ResultSet rs = row("\"d\",\"t\",\"z\",\"n\",\"m\"",
                lit("d", "2026-11-16", "date") + "," + lit("t", "2026-11-16T10:20:30", "dateTime") + ","
                        + lit("z", "2026-11-16T10:20:30+09:00", "dateTime") + "," + lit("n", "007", "integer"));
        assertEquals(LocalDate.of(2026, 11, 16), rs.getObject("d", LocalDate.class));
        assertEquals(LocalDateTime.of(2026, 11, 16, 10, 20, 30), rs.getObject("t", LocalDateTime.class));
        assertEquals(OffsetDateTime.of(2026, 11, 16, 10, 20, 30, 0, ZoneOffset.ofHours(9)), rs.getObject("z", OffsetDateTime.class));
        assertEquals(Integer.valueOf(7), rs.getObject("n", Integer.class));
        assertEquals(Long.valueOf(7), rs.getObject("n", Long.class));
        assertEquals(new BigDecimal("7"), rs.getObject("n", BigDecimal.class));
        assertEquals("007", rs.getObject("n", String.class));
        assertNull(rs.getObject("m", LocalDate.class));
        assertTrue(rs.wasNull());
    }

    @Test
    public void nStringAndUrlAndUnwrap() throws Exception {
        ResultSet rs = row("\"s\",\"u\"", lit("s", "화성", "string") + ",\"u\":{\"type\":\"uri\",\"value\":\"http://ex/a\"}");
        assertEquals("화성", rs.getNString("s"));
        assertEquals("http://ex/a", rs.getURL("u").toString());
        assertTrue(rs.unwrap(ResultSet.class) == rs);
        assertNull(rs.getWarnings());
        rs.clearWarnings();
    }

    @Test
    public void booleanOneIsTrue() {
        assertEquals(Boolean.TRUE, new LiteralObj("1", null, XSD + "boolean").getValue());
        assertEquals(Boolean.FALSE, new LiteralObj("0", null, XSD + "boolean").getValue());
    }

    @Test
    public void getStringReturnsLexicalForm() throws Exception {
        ResultSet rs = row("\"g\",\"n\"", lit("g", "--05", "gMonth") + "," + lit("n", "007", "integer"));
        assertEquals("--05", rs.getString("g"));
        assertEquals("007", rs.getString("n"));
    }

    // ---- Important 7: 인덱스 접근 ----

    @Test
    public void columnIndexOutOfRangeIs07009() throws Exception {
        ResultSet rs = row("\"n\"", lit("n", "1", "integer"));
        assertEquals("07009", sqlError(() -> rs.getString(0)).getSQLState());
        assertEquals("07009", sqlError(() -> rs.getString(2)).getSQLState());
    }

    @Test
    public void findColumnIsOneBasedAndThrowsWhenMissing() throws Exception {
        ResultSet rs = row("\"a\",\"b\"", lit("a", "1", "integer"));
        assertEquals(2, rs.findColumn("b"));
        sqlError(() -> rs.findColumn("zz"));
    }

    @Test
    public void unknownLabelThrows() throws Exception {
        ResultSet rs = row("\"a\"", lit("a", "1", "integer"));
        sqlError(() -> rs.getString("zz"));
    }

    // ---- Important 8: 잘못된 응답 ----

    @Test
    public void nonJsonQueryResponseIsError() {
        server.respond(200, "text/html", "<html>login</html>");
        sqlError(() -> conn.createStatement().executeQuery("SELECT * {}"));
    }

    @Test
    public void unexpectedJsonIsError() {
        server.respond(200, "application/json", "{\"foo\":1}");
        sqlError(() -> conn.createStatement().executeQuery("SELECT * {}"));
    }

    @Test
    public void malformedJsonIsError() {
        server.respond(200, "application/json", "{\"head\":{\"vars\":[1,}}");
        sqlError(() -> conn.createStatement().executeQuery("SELECT * {}"));
    }

    @Test
    public void nonNumericUpdateCountIsSQLException() {
        server.respond(200, "application/sparql-results+json",
                "{\"head\":{\"vars\":[\"c\"]},\"results\":{\"bindings\":[{\"c\":{\"type\":\"literal\",\"value\":\"abc\"}}]}}");
        sqlError(() -> conn.createStatement().executeUpdate("INSERT DATA { <http://a> <http://b> 1 }"));
    }

    @Test
    public void textUpdateResponseStillSucceeds() throws Exception {
        server.respond(200, "text/html", "<html>Update succeeded</html>");
        assertEquals(0, conn.createStatement().executeUpdate("INSERT DATA { <http://a> <http://b> 1 }"));
    }
}
