/*
MIT License

Copyright (c) 2026 jongearl

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.frotoma.jobc.fuseki;

import com.frotoma.jobc.JobcDriverException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.frotoma.jobc.rest.APISelectGetService;
import com.frotoma.jobc.rest.APISelectPostService;
import com.frotoma.jobc.rest.APIUpdatePostFusekiService;
import com.frotoma.jobc.web.WebSparqlConnection;
import com.frotoma.jobc.web.WebSparqlResultset;
import com.frotoma.jobc.web.WebSparqlStatement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FusekiSparqlStatement extends WebSparqlStatement{

    private Logger logger = LogManager.getLogger(FusekiSparqlStatement.class);

    public FusekiSparqlStatement(WebSparqlConnection sparqlConnection) {
        super(sparqlConnection);        
    }

    @Override
    public WebSparqlResultset executeQueryInternal( String sql ) throws JobcDriverException, SQLException{
        Map<String, String> param = new HashMap<String, String>();
        param.put("query", sql);
        String result = null;
        
        if( getSparqlConnection().getValue("method") == null || getSparqlConnection().getValue("method").equals("get")){
            // GET
            // GET
            APISelectGetService get = new APISelectGetService();
            result = get.api(getURLQuery(), param, getSparqlConnection().info());      
        }else{
            // POST
            APISelectPostService post = new APISelectPostService();
            result = post.api(getURLQuery(), param, getSparqlConnection().info());            
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
        APIUpdatePostFusekiService post = new APIUpdatePostFusekiService();
        jsonString = post.api(getURLUpdate(), param, getSparqlConnection().info());
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
