package com.frotoma.jobc.fuseki;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import com.frotoma.jobc.builder.PrepareSparqlBuilder;

/**
 * TODO : SparqlPreparedStatement 를 상속받아 구현 가능할 듯
 */
public class FusekiSparqlPreparedStatement implements PreparedStatement{

    private PrepareSparqlBuilder queryBuilder = null;

    private FusekiSparqlStatement statement = null;

    public FusekiSparqlPreparedStatement(FusekiSparqlConnection sparqlConnection, String sql){        
        this.statement = new FusekiSparqlStatement( sparqlConnection );        
        queryBuilder = new PrepareSparqlBuilder(sql);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {        
        return statement.executeQuery(queryBuilder.query());
    }

    @Override
    public int executeUpdate() throws SQLException {        
        return statement.executeUpdate(queryBuilder.query());
    }
    
    @Override
    public boolean execute() throws SQLException {                
        return statement.execute(queryBuilder.query());
    }
    
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return statement.getResultSet().getMetaData();
    }

    @Override
    public void addBatch() throws SQLException {
        statement.addBatch(queryBuilder.query());
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        queryBuilder.addParameter(parameterIndex , "");        
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);        
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);        
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);        
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);                
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);        
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);        
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {             
        queryBuilder.addParameter(parameterIndex , x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {        
        queryBuilder.addParameter(parameterIndex , x);
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);
        
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);   
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearParameters() throws SQLException {
        //queryBuilder.clearParameters();        
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);        
    }
    

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);                
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);                
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);                
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        queryBuilder.addParameter(parameterIndex , "");        
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        queryBuilder.addParameter(parameterIndex , x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        queryBuilder.addParameter(parameterIndex , value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
     
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {        
        return statement.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {        
        return statement.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {        
        return statement.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        statement.setMaxFieldSize(max);        
    }

    @Override
    public int getMaxRows() throws SQLException {        
        return statement.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {        
        statement.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        statement.setEscapeProcessing(enable);        
    }

    @Override
    public int getQueryTimeout() throws SQLException {        
        return statement.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        statement.setQueryTimeout(seconds);        
    }

    @Override
    public void cancel() throws SQLException {
        statement.cancel();
        
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {        
        return statement.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        statement.clearWarnings();        
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        statement.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return statement.execute(sql);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {        
        return statement.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return statement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {        
        return statement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        statement.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return statement.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        statement.setFetchSize(rows);        
    }

    @Override
    public int getFetchSize() throws SQLException {
        return statement.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return statement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return statement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        statement.addBatch(sql);        
    }

    @Override
    public void clearBatch() throws SQLException {
        statement.clearBatch();        
    }

    @Override
    public int[] executeBatch() throws SQLException {        
        return statement.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return statement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return statement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {        
        return statement.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return statement.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return statement.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return statement.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return statement.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return statement.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {        
        return statement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return statement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        statement.setPoolable(poolable);        
    }

    @Override
    public boolean isPoolable() throws SQLException {        
        return statement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        statement.closeOnCompletion();        
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {        
        return statement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {        
        return statement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return statement.isWrapperFor(iface);
    }
    
}
