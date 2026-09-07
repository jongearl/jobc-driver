package com.frotoma.jobc.fuseki;

import com.frotoma.jobc.rest.APIMangerService;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import com.frotoma.jobc.web.WebSparqlConnection;

public class FusekiSparqlConnection extends WebSparqlConnection{

    public FusekiSparqlConnection(String url, APIMangerService apiManger) {
        super( url, apiManger );
        try {
            setReadOnly(false);
        } catch (SQLException e) {}
    }

    @Override
    public Statement createStatement() throws SQLException {        
        return new FusekiSparqlStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {        
        return new FusekiSparqlPreparedStatement(this, sql);
    }
}
