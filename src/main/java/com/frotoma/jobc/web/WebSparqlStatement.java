package com.frotoma.jobc.web;

import com.frotoma.jobc.JobcDriverException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private int queryTimeoutSeconds = 0;

    private volatile boolean closed = false;

    private final List<String> batch = new ArrayList<>();


    public WebSparqlStatement(WebSparqlConnection sparqlConnection) {
        this.sparqlConnection = sparqlConnection;
    }

    /** 이 Statement 나 연결이 닫혔으면 예외 */
    protected void checkOpen() throws SQLException {
        if( closed ){
            throw new SQLException("Statement is closed", "HY010");
        }
        sparqlConnection.checkOpen();
    }

    /** 요청 응답 대기 시간(ms): setQueryTimeout 이 있으면 그것, 없으면 연결의 networkTimeout(0이면 접속 기본값) */
    protected int effectiveTimeoutMillis() throws SQLException {
        return queryTimeoutSeconds > 0 ? queryTimeoutSeconds * 1000 : sparqlConnection.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if( iface.isInstance(this) ){
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    public WebSparqlResultset executeQueryInternal( String sql ) throws JobcDriverException, SQLException{
        Map<String, String> param = new HashMap<String, String>();
        param.put("query", sql);
        String result = null;
        
        if( sparqlConnection.getValue("method") == null || sparqlConnection.getValue("method").equals("get")){
            // GET            
            result = sparqlConnection.getAPIManger().get(sparqlConnection.getUrl(), param, effectiveTimeoutMillis());
            //APISelectGetService get = new APISelectGetService();
            //result = get.api(sparqlConnection.getUrl(), param, sparqlConnection.info());      
        }else{
            // POST
            result = sparqlConnection.getAPIManger().post(sparqlConnection.getUrl(), param, effectiveTimeoutMillis());
//            APISelectPostService post = new APISelectPostService();
//            result = post.api(sparqlConnection.getUrl(), param, sparqlConnection.info());            
        }
        
        if( result == null ){
            this.rs =  new WebSparqlResultset(this );
        }else if( isJson(result) ){
            this.rs =  new WebSparqlResultset(this, result );
        }else{
            // HTML 로그인 페이지, Turtle(CONSTRUCT/DESCRIBE), XML 등은 0건으로 숨기지 않고 오류로 알린다
            throw new SQLException("Unexpected response: not SPARQL JSON results", "HY000");
        }
        return rs;

    }

    /**
     * 쿼리실행하기
     */
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen();
        try {
            this.rs = executeQueryInternal(sql);
            return rs;
        } catch (JobcDriverException ex) {        
            logger.error( ex.toString() );
            throw ex.toSQLException();
        }
    }

    public String updateQueryInternal( String sql ) throws SQLException{
        try {
            Map<String, String> param = new HashMap<String, String>();
            param.put("query", sql);
            String jsonString = null;
            // jsonString  = restAPIService.post(sparqlConnection.getUrl(), param, sparqlConnection.info());
            
            // POST
            jsonString = sparqlConnection.getAPIManger().post(sparqlConnection.getUrl(), param, effectiveTimeoutMillis());
//            APISelectPostService post = new APISelectPostService();
//            jsonString = post.api(sparqlConnection.getUrl(), param, sparqlConnection.info());
            return jsonString;
        } catch (JobcDriverException ex) {
            logger.error( ex.toString() );
            throw ex.toSQLException();
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        checkOpen();
        if( sparqlConnection.isReadOnly() ){
            throw new SQLException("Read Only");
        }
        
        String jsonString;
        try {
            jsonString = updateQueryInternal(sql);
        } catch (JobcDriverException ex) {
            logger.error( ex.toString() );
            throw ex.toSQLException();
        }

        logger.debug("RESPONSE JSON : "+jsonString);

        if( jsonString == null ){
            updateCount = 0;
            return updateCount;
        }else if( isJson(jsonString) ){
            try {
                this.rs =  new WebSparqlResultset(this, jsonString );
            } catch (SQLException e) {
                if( !"HY000".equals(e.getSQLState()) ){
                    throw e;
                }
                // 결과 형식이 아닌 JSON 응답(서버 상태 메시지 등): update 는 성공으로 본다
                updateCount = 0;
                return updateCount;
            }
        }else{
            updateCount = 0;
            return updateCount;
        }

        // 이전 버전에서는 update 건수를 결과로 리턴 했음
        while( rs.next() ){
            Object obj = rs.getObject(1);
            Object value = ( obj instanceof LiteralObj ) ? ((LiteralObj)obj).getValue() : obj;
            if( value instanceof Number ){
                updateCount = ((Number) value).intValue();
            }else if( value != null ){
                try {
                    updateCount = Integer.parseInt(value.toString().trim());
                } catch (NumberFormatException e) {
                    throw new SQLException("Update count is not a number: '" + value + "'", "22018", e);
                }
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
    public void close() throws SQLException {
        closed = true;
        batch.clear();
    }

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
        return queryTimeoutSeconds;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        if( seconds < 0 ){
            throw new SQLException("Query timeout must be >= 0: " + seconds);
        }
        this.queryTimeoutSeconds = seconds;
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
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    /** 바로 실행하지 않고 쌓아 둔다. executeBatch() 에서 차례로 실행한다. */
    public void addBatch(String sql) throws SQLException {
        checkOpen();
        batch.add(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        batch.clear();
    }

    /** 쌓아 둔 update 를 차례로 실행한다. 중간에 실패하면 그때까지의 결과를 담은 BatchUpdateException. 끝나면 비운다. */
    @Override
    public int[] executeBatch() throws SQLException {
        checkOpen();
        int[] counts = new int[batch.size()];
        try {
            for( int i = 0; i < counts.length; i++ ){
                try {
                    counts[i] = executeUpdate(batch.get(i));
                } catch (SQLException e) {
                    throw new BatchUpdateException(e.getMessage(), e.getSQLState(), e.getErrorCode(), Arrays.copyOf(counts, i), e);
                }
            }
            return counts;
        } finally {
            batch.clear();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {        
        return this.sparqlConnection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
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
