package com.frotoma.jobc.fuseki;

import com.frotoma.jobc.JobcDriverException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frotoma.jobc.web.WebSparqlConnection;
import com.frotoma.jobc.web.WebSparqlResultset;
import com.frotoma.jobc.web.WebSparqlStatement;


public class FusekiSparqlStatement extends WebSparqlStatement{

    private Logger logger = LoggerFactory.getLogger(FusekiSparqlStatement.class);

    public FusekiSparqlStatement(WebSparqlConnection sparqlConnection) {
        super(sparqlConnection);        
    }

    @Override
    public WebSparqlResultset executeQueryInternal( String sql ) throws JobcDriverException, SQLException{
        Map<String, String> param = new HashMap<String, String>();
        param.put("query", sql);
        String result = null;

        logger.debug( "SPARQL : " + sql );
        
        if( getSparqlConnection().getValue("method") == null || getSparqlConnection().getValue("method").equals("get")){
            // GET
            result = getSparqlConnection().getAPIManger().get(getURLQuery(), param);
//            APISelectGetService get = new APISelectGetService();
//            result = get.api(getURLQuery(), param, getSparqlConnection().info());      
        }else{
            // POST
            result = getSparqlConnection().getAPIManger().post(getURLQuery(), param);
//            APISelectPostService post = new APISelectPostService();
//            result = post.api(getURLQuery(), param, getSparqlConnection().info());            
        }

        WebSparqlResultset rs = null;
        if( result == null ){
            rs =  new WebSparqlResultset(this );
        }else if( isJson(result) ){
            rs =  new WebSparqlResultset(this, result );
        }else{
            rs =  new WebSparqlResultset(this );
        }
        return rs;
    }


    @Override
    public String updateQueryInternal( String sql ) throws SQLException{
        Map<String, String> param = new HashMap<String, String>();
        param.put("update", sql);        
        String jsonString = null;
        // jsonString  = restAPIService.post(sparqlConnection.getUrl(), param, sparqlConnection.info());

        // POST            
        jsonString = getSparqlConnection().getAPIManger().post(getURLUpdate(), param);
//        APIUpdatePostFusekiService post = new APIUpdatePostFusekiService();
//        jsonString = post.api(getURLUpdate(), param, getSparqlConnection().info());
        return jsonString;
    }

   
    public String getURLQuery(){
        return generateURL("/sparql");
    }

    public String getURLUpdate(){        
        return generateURL("/update");
    }
    
    private String generateURL(String page){
        String sparql_server_url = getSparqlConnection().getUrl().toLowerCase();
        if( sparql_server_url.endsWith("/sparql") ){
            int endIdx = sparql_server_url.lastIndexOf("/sparql");
            sparql_server_url = sparql_server_url.substring(0, endIdx)+page;
            
        }else{
            sparql_server_url += page;
        }    
        return sparql_server_url;
    }
    
}
