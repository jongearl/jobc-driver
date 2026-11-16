package com.frotoma.jobc.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.frotoma.jobc.JobcDriver;
import com.frotoma.jobc.MockSparqlServer;
import com.frotoma.jobc.fuseki.FusekiSparqlDriver;
import com.frotoma.jobc.rest.APIMangerService;

public class DriverLifecycleFixTest {

    private MockSparqlServer server;

    @Before
    public void setUp() throws Exception {
        server = MockSparqlServer.start();
    }

    @After
    public void tearDown() {
        server.close();
    }

    @Test
    public void urlPasswordWithEqualsAndPercentIsDecoded() throws Exception {
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver().connect(
                "jobc:fuseki:" + server.base() + "/ds/sparql&user=u&password=a%3Db=c", new Properties());
        c.createStatement().executeQuery("ASK {}");

        String expected = "Basic " + Base64.getEncoder().encodeToString("u:a=b=c".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, server.lastHeader("Authorization"));
        assertEquals(server.base() + "/ds/sparql", c.getUrl());
    }

    @Test
    public void cacheKeyHidesPassword() {
        Properties p = new Properties();
        p.setProperty("password", "topsecret");
        String key = AbstractHttpSparqlDriver.cacheKey("http://h/ds", p);
        assertFalse(key, key.contains("topsecret"));
        assertTrue(key.startsWith("http://h/ds#"));
    }

    @Test
    public void poolClosedOnLastClose() throws Exception {
        FusekiSparqlDriver driver = new FusekiSparqlDriver();
        String url = "jobc:fuseki:" + server.base() + "/ds/sparql";
        WebSparqlConnection c1 = (WebSparqlConnection) driver.connect(url, new Properties());
        WebSparqlConnection c2 = (WebSparqlConnection) driver.connect(url, new Properties());
        APIMangerService api = c1.getAPIManger();
        assertSame(api, c2.getAPIManger());

        c1.close();
        assertFalse(api.isClosed());
        c2.close();
        assertTrue(api.isClosed());

        WebSparqlConnection c3 = (WebSparqlConnection) driver.connect(url, new Properties());
        assertNotSame(api, c3.getAPIManger());
        assertTrue(c3.createStatement().executeQuery("ASK {}") != null);
    }

    @Test
    public void closeIsIdempotent() throws Exception {
        FusekiSparqlDriver driver = new FusekiSparqlDriver();
        String url = "jobc:fuseki:" + server.base() + "/ds/sparql";
        WebSparqlConnection c1 = (WebSparqlConnection) driver.connect(url, new Properties());
        WebSparqlConnection c2 = (WebSparqlConnection) driver.connect(url, new Properties());

        c1.close();
        c1.close();

        assertTrue(c1.isClosed());
        assertFalse(c2.getAPIManger().isClosed());
        c2.createStatement().executeQuery("ASK {}");
    }

    @Test
    public void closedConnectionRejectsWork() throws Exception {
        WebSparqlConnection c = (WebSparqlConnection) new FusekiSparqlDriver()
                .connect("jobc:fuseki:" + server.base() + "/ds/sparql", new Properties());
        Statement before = c.createStatement();
        c.close();

        try {
            c.createStatement();
            fail();
        } catch (SQLException e) {
            assertEquals("08003", e.getSQLState());
        }
        try {
            before.executeQuery("ASK {}");
            fail();
        } catch (SQLException e) {
            assertEquals("08003", e.getSQLState());
        }
    }

    @Test
    public void driverRegisteredOnce() throws Exception {
        Class.forName("com.frotoma.jobc.fuseki.FusekiSparqlDriver");
        Class.forName("com.frotoma.jobc.web.WebSparqlDriver");
        Class.forName("com.frotoma.jobc.virt.VirtuosoSparqlDriver");
        int count = 0;
        for (Driver d : Collections.list(DriverManager.getDrivers())) {
            if (d.acceptsURL("jobc:fuseki:http://x/ds")) {
                count++;
            }
        }
        assertEquals(1, count);
    }

    @Test
    public void propertyInfoNotNull() throws Exception {
        assertEquals(0, new JobcDriver().getPropertyInfo("jobc:fuseki:http://x", new Properties()).length);
        assertEquals(0, new FusekiSparqlDriver().getPropertyInfo("jobc:fuseki:http://x", new Properties()).length);
    }
}
