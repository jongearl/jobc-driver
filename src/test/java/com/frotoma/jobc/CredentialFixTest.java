package com.frotoma.jobc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.frotoma.jobc.fuseki.FusekiSparqlDriver;
import com.frotoma.jobc.virt.VirtuosoSparqlDriver;
import com.frotoma.jobc.web.WebSparqlConnection;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class CredentialFixTest {

    private MockSparqlServer server;
    private ListAppender<ILoggingEvent> appender;
    private Logger root;
    private Logger frotoma;
    private Level frotomaLevel;

    @Before
    public void setUp() throws Exception {
        server = MockSparqlServer.start();
        root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        frotoma = (Logger) LoggerFactory.getLogger("com.frotoma");
        frotomaLevel = frotoma.getLevel();
        frotoma.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
    }

    @After
    public void tearDown() {
        root.detachAppender(appender);
        frotoma.setLevel(frotomaLevel);
        server.close();
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    // ---- LogMasker ----

    @Test
    public void masksVirtuosoPwd() {
        assertEquals("jdbc:virtuoso://h:1111/UID=dba/PWD=****/charset=UTF-8",
                LogMasker.maskUrl("jdbc:virtuoso://h:1111/UID=dba/PWD=s3cr=t/charset=UTF-8"));
    }

    @Test
    public void masksQueryPassword() {
        assertEquals("http://h/ds&user=u&password=****&method=post",
                LogMasker.maskUrl("http://h/ds&user=u&password=a%3Db&method=post"));
    }

    @Test
    public void masksUserInfo() {
        assertEquals("http://u:****@h/ds", LogMasker.maskUrl("http://u:p@h/ds"));
    }

    @Test
    public void maskNull() {
        assertNull(LogMasker.maskUrl(null));
    }

    // ---- 인증정보 ----

    @Test
    public void credentialsSentAsBasicAuth() throws Exception {
        Properties p = new Properties();
        p.setProperty("user", "u1");
        p.setProperty("password", "p2");
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver()
                .connect("jobc:fuseki:" + server.base() + "/ds/sparql", p);

        c.createStatement().executeQuery("ASK {}");

        assertEquals(basic("u1", "p2"), server.lastHeader("Authorization"));
    }

    @Test
    public void passwordNotReadableFromConnection() throws Exception {
        Properties p = new Properties();
        p.setProperty("user", "u1");
        p.setProperty("password", "p2");
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver()
                .connect("jobc:fuseki:" + server.base() + "/ds/sparql", p);

        assertNull(c.getValue("password"));
    }

    @Test
    public void noFrotomaLogContainsSecret() throws Exception {
        Properties p = new Properties();
        p.setProperty("user", "u");
        p.setProperty("password", "topsecret");
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver()
                .connect("jobc:fuseki:" + server.base() + "/ds/sparql&method=post", p);
        c.createStatement().executeQuery("ASK {}");

        try {
            new VirtuosoSparqlDriver().connect("jobc:virtuoso://127.0.0.1:1/UID=dba/PWD=topsecret", new Properties());
            fail("접속이 실패해야 한다");
        } catch (SQLException expected) {
            // 서버 없음
        }

        String b64 = Base64.getEncoder().encodeToString("u:topsecret".getBytes(StandardCharsets.UTF_8));
        for (ILoggingEvent e : appender.list) {
            if (!e.getLoggerName().startsWith("com.frotoma")) continue;
            String msg = e.getFormattedMessage();
            assertFalse(e.getLoggerName() + ": " + msg, msg.contains("topsecret"));
            assertFalse(e.getLoggerName() + ": " + msg, msg.contains(b64));
        }
    }

    @Test
    public void warnsOnBasicAuthOverPlainHttp() throws Exception {
        Properties p = new Properties();
        p.setProperty("user", "u");
        p.setProperty("password", "x");
        new FusekiSparqlDriver().connect("jobc:fuseki:" + server.base() + "/warn/sparql", p);

        long warns = appender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("plain HTTP"))
                .count();
        assertTrue("warn count " + warns, warns == 1);
    }
}
