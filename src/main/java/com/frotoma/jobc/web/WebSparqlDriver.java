package com.frotoma.jobc.web;

import com.frotoma.jobc.JobcDriver;
import com.frotoma.jobc.rest.APIMangerService;

public class WebSparqlDriver extends AbstractHttpSparqlDriver {

    public static final String DRIVER_WEB_URL_STARTWITH = "jobc:web:";

    static {
        // DriverManager 등록은 JobcDriver 한 곳에서만 한다
        JobcDriver.ensureRegistered();
    }

    @Override
    protected String urlPrefix() {
        return DRIVER_WEB_URL_STARTWITH;
    }

    @Override
    protected WebSparqlConnection newConnection(String serverUrl, APIMangerService api) {
        return new WebSparqlConnection(serverUrl, api);
    }
}
