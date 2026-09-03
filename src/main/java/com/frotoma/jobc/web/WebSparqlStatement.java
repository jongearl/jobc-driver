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
package com.frotoma.jobc.web;

import com.frotoma.jobc.JobcDriverException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.frotoma.jobc.obj.LiteralObj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSparqlStatement implements Statement{

    private static final Pattern SPARQL_PROLOGUE = Pattern.compile(
            "(?is)^(?:PREFIX\\s+[^\\s:]*:\\s*<[^>]*>|BASE\\s*<[^>]*>)\\s*");
    
    private static final Logger logger = LoggerFactory.getLogger(WebSparqlStatement.class);
    
    private WebSparqlConnection sparqlConnection = null;

    private int maxrows = 0;
    private int maxFieldSize = 0;

    private WebSparqlResultset rs = null;

    private int updateCount = -1;


    public WebSparqlStatement(WebSparqlConnection sparqlConnection) {
        this.sparqlConnection = sparqlConnection;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    public WebSparqlResultset executeQueryInternal( String sql ) throws JobcDriverException, SQLException{
        Map<String, String> param = new HashMap<String, String>();
        param.put("query", sql);
        String result = null;
        
        if( sparqlConnection.getValue("method") == null || sparqlConnection.getValue("method").equals("get")){
            // GET            
            result = sparqlConnection.getAPIManger().get(sparqlConnection.getUrl(), param);
            //APISelectGetService get = new APISelectGetService();
            //result = get.api(sparqlConnection.getUrl(), param, sparqlConnection.info());      
        }else{
            // POST
            result = sparqlConnection.getAPIManger().post(sparqlConnection.getUrl(), param);
//            APISelectPostService post = new APISelectPostService();
//            result = post.api(sparqlConnection.getUrl(), param, sparqlConnection.info());            
        }
        
        if( result == null ){
            this.rs =  new WebSparqlResultset(this );
        }else if( isJson(result) ){
            this.rs =  new WebSparqlResultset(this, result );
        }else{
            this.rs =  new WebSparqlResultset(this );
        }
        return rs;

    }

    /**
     * 쿼리실행하기
     */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        try {
            this.rs = executeQueryInternal(sql);
            return rs;
        } catch (JobcDriverException ex) {        
            logger.error( ex.toString() );
            throw new SQLException(ex);
        }
    }

    public String updateQueryInternal( String sql ) throws SQLException{
        try {
            Map<String, String> param = new HashMap<String, String>();
            param.put("query", sql);
            String jsonString = null;
            // jsonString  = restAPIService.post(sparqlConnection.getUrl(), param, sparqlConnection.info());
            
            // POST
            jsonString = sparqlConnection.getAPIManger().post(sparqlConnection.getUrl(), param);
//            APISelectPostService post = new APISelectPostService();
//            jsonString = post.api(sparqlConnection.getUrl(), param, sparqlConnection.info());
            return jsonString;
        } catch (JobcDriverException ex) {
            logger.error( ex.toString() );
            throw new SQLException(ex);
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {        
        if( sparqlConnection.isReadOnly() ){
            throw new SQLException("Read Only");
        }
        
        String jsonString = updateQueryInternal(sql);        

        logger.debug("RESPONSE JSON : "+jsonString);

        if( jsonString == null ){
            updateCount = 0;
            return updateCount;
        }else if( isJson(jsonString) ){
            this.rs =  new WebSparqlResultset(this, jsonString );
        }else{
            updateCount = 0;
            return updateCount;
        }

        // 이전 버전에서는 update 건수를 결과로 리턴 했음
        while( rs.next() ){
            Object obj = rs.getObject(1);
            if( obj instanceof LiteralObj ){
                LiteralObj data = (LiteralObj)obj;
                updateCount = (Integer)data.getValue();
            }else if( obj instanceof String ){                
                updateCount = Integer.parseInt(obj.toString());
            }
        }

        // 결과가 없다면 updateCount를 0으로 설정 (통과했기 때문에)
        if( updateCount < 0 ){
            updateCount = 0;
        }
        rs.close();

        return updateCount;
    }

    public WebSparqlConnection getSparqlConnection(){
        return sparqlConnection;
    }

    public void setUpdateCount(int updateCount){
        this.updateCount = updateCount;
    }

    @Override
    public void close() throws SQLException {}

    @Override
    public int getMaxFieldSize() throws SQLException {
        return maxFieldSize;
    }

    @Override
    public void setMaxFieldSize(int maxFieldSize) throws SQLException {
        this.maxFieldSize = maxFieldSize;        
    }

    @Override
    public int getMaxRows() throws SQLException {        
        return maxrows;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        this.maxrows = max;
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        // TODO Auto-generated method stub        
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cancel() throws SQLException {}

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {}

    @Override
    public void setCursorName(String name) throws SQLException {        }

    @Override
    public boolean execute(String sql) throws SQLException {

        String normalizedSql = normalizeSql(sql);
        boolean isQuery = true;
        if( normalizedSql.startsWith("SELECT") ){
            isQuery = true;
        }else if( normalizedSql.startsWith("CONSTRUCT")){
            isQuery = true;
        }else if( normalizedSql.startsWith("ASK")){
            isQuery = true;    
        }else if( normalizedSql.startsWith("DESCRIBE")){
            isQuery = true;
        }else if( normalizedSql.startsWith("CREATE") ){
            isQuery = false;
        }else if( normalizedSql.startsWith("DROP") ){
            isQuery = false;
        }else if( normalizedSql.startsWith("INSERT") ){
            isQuery = false;
        }else if( normalizedSql.startsWith("DELETE") ){
            isQuery = false;
        }else{
            isQuery = false;
        }

        if( isQuery ){
            WebSparqlResultset rs = (WebSparqlResultset)executeQuery(sql);
            return ( rs.size() > 0 );
        }else{
            int i = executeUpdate(sql);
            return i > 0;
        }        
    }

    private String normalizeSql(String sql){
        String normalizedSql = sql == null ? "" : sql.trim();
        Matcher matcher = SPARQL_PROLOGUE.matcher(normalizedSql);
        while( matcher.find() ){
            normalizedSql = normalizedSql.substring(matcher.end()).trim();
            matcher = SPARQL_PROLOGUE.matcher(normalizedSql);
        }
        return normalizedSql.toUpperCase(Locale.ROOT);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return rs;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return updateCount;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return rs != null;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        // TODO 
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return 0;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {        
    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getResultSetType() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addBatch(String sql) throws SQLException {    
        this.execute(sql);
    }

    @Override
    public void clearBatch() throws SQLException {        
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection getConnection() throws SQLException {        
        return this.sparqlConnection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isPoolable() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean isJson(String json){
        if( json == null || json.isEmpty() ){
            return false;
        }

        int start = 0;
        int end = json.length() - 1;
        while( start <= end && Character.isWhitespace(json.charAt(start)) ){
            start++;
        }
        while( end >= start && Character.isWhitespace(json.charAt(end)) ){
            end--;
        }

        if( start > end ){
            return false;
        }

        char first = json.charAt(start);
        char last = json.charAt(end);
        if( (first != '{' || last != '}') && (first != '[' || last != ']') ){
            return false;
        }

        char[] bracketStack = new char[end - start + 1];
        int stackDepth = 0;
        boolean quoted = false;
        boolean escaped = false;

        for( int i = start; i <= end; i++ ){
            char ch = json.charAt(i);

            if( quoted ){
                if( escaped ){
                    escaped = false;
                }else if( ch == '\\' ){
                    escaped = true;
                }else if( ch == '"' ){
                    quoted = false;
                }else if( ch < 0x20 ){
                    return false;
                }
                continue;
            }

            if( ch == '"' ){
                quoted = true;
            }else if( ch == '{' ){
                bracketStack[stackDepth++] = ch;
            }else if( ch == '}' ){
                if( stackDepth == 0 || bracketStack[--stackDepth] != '{' ){
                    return false;
                }
            }else if( ch == '[' ){
                bracketStack[stackDepth++] = ch;
            }else if( ch == ']' ){
                if( stackDepth == 0 || bracketStack[--stackDepth] != '[' ){
                    return false;
                }
            }
        }

        return !quoted && !escaped && stackDepth == 0;
    }
    
}
