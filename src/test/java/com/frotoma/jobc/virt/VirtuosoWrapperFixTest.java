package com.frotoma.jobc.virt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class VirtuosoWrapperFixTest {

    private static final String SELECT = "SELECT ?s WHERE { ?s <http://ex/p> ? }";

    private final List<String> executed = new ArrayList<>();
    private Connection conn;

    /** 실행된 SQL 을 기록만 하는 가짜 Statement */
    private Statement fakeStatement() {
        return (Statement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Statement.class },
                (proxy, method, args) -> {
                    if (method.getName().startsWith("execute") && args != null && args[0] instanceof String) {
                        executed.add((String) args[0]);
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
    }

    @Before
    public void setUp() {
        Connection fake = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("createStatement")) return fakeStatement();
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
        conn = new VirtuosoConnection(fake);
    }

    @Test
    public void typedCreateStatementAddsSparqlPrefix() throws Exception {
        conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery("SELECT * WHERE {?s ?p ?o}");
        conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT)
                .executeQuery("SELECT * WHERE {?s ?p ?o}");
        assertTrue(executed.get(0), executed.get(0).startsWith("SPARQL SELECT"));
        assertTrue(executed.get(1), executed.get(1).startsWith("SPARQL SELECT"));
    }

    @Test
    public void preparedStringEscaped() throws Exception {
        PreparedStatement ps = conn.prepareStatement(SELECT);
        ps.setString(1, "x\" } DROP ALL #");
        ps.executeQuery();
        assertTrue(executed.get(0), executed.get(0).contains("x\\\" } DROP ALL #"));
    }

    @Test
    public void clearParametersWorks() throws Exception {
        PreparedStatement ps = conn.prepareStatement(SELECT);
        ps.setString(1, "abc");
        ps.clearParameters();
        try {
            ps.executeQuery();
            fail("값이 지워졌으므로 07001");
        } catch (SQLException e) {
            assertEquals("07001", e.getSQLState());
        }
        ps.setString(1, "def");
        ps.executeQuery();
        assertFalse(executed.get(0), executed.get(0).contains("abc"));
    }

    @Test
    public void setNullThrows() throws Exception {
        PreparedStatement ps = conn.prepareStatement(SELECT);
        try {
            ps.setNull(1, Types.VARCHAR);
            fail();
        } catch (SQLException e) {
            assertEquals("22004", e.getSQLState());
        }
    }
}
