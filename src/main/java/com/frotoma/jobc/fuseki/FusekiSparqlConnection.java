package com.frotoma.jobc.fuseki;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.frotoma.jobc.web.WebSparqlConnection;

public class FusekiSparqlConnection extends WebSparqlConnection{

    public FusekiSparqlConnection(String url, Properties info) {
        super( url, info );
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
