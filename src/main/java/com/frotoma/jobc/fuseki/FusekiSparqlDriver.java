package com.frotoma.jobc.fuseki;

import com.frotoma.jobc.JobcDriver;
import com.frotoma.jobc.rest.APIMangerService;
import com.frotoma.jobc.web.AbstractHttpSparqlDriver;
import com.frotoma.jobc.web.WebSparqlConnection;

public class FusekiSparqlDriver extends AbstractHttpSparqlDriver {

    public static final String DRIVER_FUSEKI_URL_STARTWITH = "jobc:fuseki:";

    static {
        // DriverManager 등록은 JobcDriver 한 곳에서만 한다
        JobcDriver.ensureRegistered();
    }

    @Override
    protected String urlPrefix() {
        return DRIVER_FUSEKI_URL_STARTWITH;
    }

    @Override
    protected WebSparqlConnection newConnection(String serverUrl, APIMangerService api) {
        return new FusekiSparqlConnection(serverUrl, api);
    }
}
