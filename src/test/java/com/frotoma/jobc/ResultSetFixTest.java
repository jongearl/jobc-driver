package com.frotoma.jobc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.frotoma.jobc.fuseki.FusekiSparqlDriver;
import com.frotoma.jobc.obj.LiteralObj;

public class ResultSetFixTest {

    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";

    private static String lit(String var, String value, String type) {
        return "\"" + var + "\":{\"type\":\"literal\",\"value\":\"" + value + "\",\"datatype\":\"" + XSD + type + "\"}";
    }

    private static final String JSON = "{\"head\":{\"vars\":[\"n\",\"m\",\"b\",\"d\",\"big\"]},\"results\":{\"bindings\":[{"
            + lit("n", "007", "integer") + ","
            + lit("b", "true", "boolean") + ","
            + lit("d", "1.5", "decimal") + ","
            + lit("big", "12345678901234567890", "integer")
            + "}]}}";

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

    private ResultSet row() throws SQLException {
        server.respond(200, "application/sparql-results+json", JSON);
        ResultSet rs = conn.createStatement().executeQuery("SELECT * {}");
        assertTrue(rs.next());
        return rs;
    }

    @Test
    public void unboundIntIsZeroAndWasNull() throws Exception {
        ResultSet rs = row();
        assertEquals(0, rs.getInt("m"));
        assertTrue(rs.wasNull());
    }

    @Test
    public void boundIntClearsWasNull() throws Exception {
        ResultSet rs = row();
        rs.getInt("m");
        assertEquals(7, rs.getInt("n"));
        assertFalse(rs.wasNull());
    }

    @Test
    public void unboundBooleanFalse() throws Exception {
        ResultSet rs = row();
        assertFalse(rs.getBoolean("m"));
        assertTrue(rs.wasNull());
    }

    @Test
    public void booleanTyped() throws Exception {
        assertTrue(row().getBoolean("b"));
    }

    @Test
    public void decimalAsDouble() throws Exception {
        assertEquals(1.5, row().getDouble("d"), 0.0);
    }

    @Test
    public void longFromInteger() throws Exception {
        assertEquals(7L, row().getLong("n"));
    }

    @Test
    public void intOverflowIs22003() throws Exception {
        ResultSet rs = row();
        try {
            rs.getInt("big");
            fail();
        } catch (SQLException e) {
            assertEquals("22003", e.getSQLState());
        }
    }

    @Test
    public void updateCountFromLong() throws Exception {
        server.respond(200, "application/sparql-results+json",
                "{\"head\":{\"vars\":[\"c\"]},\"results\":{\"bindings\":[{" + lit("c", "5", "long") + "}]}}");
        assertEquals(5, conn.createStatement().executeUpdate("INSERT DATA { <http://a> <http://b> 1 }"));
    }

    // ---- LiteralObj ----

    @Test
    public void literalToStringEscapes() {
        assertEquals("\"a\\\"b\\\\c\"^^<http://www.w3.org/2001/XMLSchema#string>",
                new LiteralObj("a\"b\\c", null, XSD + "string").toString());
    }

    @Test
    public void literalToStringKeepsLexical() {
        assertEquals("\"007\"^^<" + XSD + "integer>", new LiteralObj("007", null, XSD + "integer").toString());
    }

    @Test
    public void literalToStringLang() {
        assertEquals("\"화성\"@ko", new LiteralObj("화성", "ko", null).toString());
    }

    @Test
    public void gYearMonthIsYearMonth() {
        assertEquals(YearMonth.of(2026, 5), new LiteralObj("2026-05", null, XSD + "gYearMonth").getValue());
    }

    @Test
    public void gMonthDayIsMonthDay() {
        assertEquals(MonthDay.of(5, 12), new LiteralObj("--05-12", null, XSD + "gMonthDay").getValue());
    }

    @Test
    public void gMonthIsInteger() {
        assertEquals(5, new LiteralObj("--05", null, XSD + "gMonth").getValue());
    }

    @Test
    public void gDayIsInteger() {
        assertEquals(12, new LiteralObj("---12", null, XSD + "gDay").getValue());
    }

    @Test
    public void gYearWithTimezone() {
        LiteralObj l = new LiteralObj("2026Z", null, XSD + "gYear");
        assertEquals(2026, l.getValue());
        assertEquals("2026Z", l.getLexicalForm());
    }

    @Test
    public void gYearMonthWithOffset() {
        assertEquals(YearMonth.of(2026, 5), new LiteralObj("2026-05+09:00", null, "xsd:gYearMonth").getValue());
    }
}
