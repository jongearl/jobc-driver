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

import com.frotoma.jobc.obj.LiteralObj;
import com.frotoma.jobc.obj.ResourceObj;
import com.google.gson.JsonArray;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


import org.json.JSONArray;
import org.json.JSONObject;

public class WebSparqlResultset implements ResultSet{
    
    private Statement stmt = null;

    private List<String> labels = null;

    private JSONArray bindings = null;

    private int lastIdx = 0;

    private int index = -1;

    public WebSparqlResultset(Statement stmt, String jsonString) throws SQLException {
        
        this.stmt = stmt;
        
        // Sparql JSON 파싱        
        JSONObject  jsonParser = new JSONObject (jsonString);        
        
        // 접속 에러
        if( jsonParser.has("status") ){
            if( jsonParser.has("error") && jsonParser.getInt("status") != 200 ){                
                throw new SQLException(jsonString);
            }
        }


        if( jsonParser.has("head") && jsonParser.has("boolean") ){            
            String columnLabel = "ask";
            this.labels = new ArrayList<String>();
            this.labels.add(columnLabel);
            this.bindings = new JSONArray();

            JSONObject binding = new JSONObject();
            binding.put(columnLabel, new JSONObject()
                    .put("type", "literal")
                    .put("value", String.valueOf(jsonParser.getBoolean("boolean"))));
            this.bindings.put(binding);

            this.lastIdx = this.bindings.length();
            this.stmt.setMaxRows(this.lastIdx);
            this.stmt.setMaxFieldSize(this.labels.size());
        }
        
        // Header 파싱        
        else if( jsonParser.has("head") && jsonParser.has("results") ){            
            JSONObject head = jsonParser.getJSONObject("head");
            JSONArray vars = head.getJSONArray("vars");
            this.labels = new ArrayList<String>();
            for( int i = 0; i< vars.length(); i++ ){
                String var = vars.getString(i);
                labels.add(var);            
            }
            // Results 파싱
            JSONObject results = jsonParser.getJSONObject("results");
            this.bindings = results.getJSONArray("bindings");
            this.lastIdx = this.bindings.length();
            this.stmt.setMaxRows(this.lastIdx);
            this.stmt.setMaxFieldSize(this.labels.size());
        }
    }

    public WebSparqlResultset(Statement stmt) throws SQLException {
        this.stmt = stmt;
        this.bindings = new JSONArray();
        this.labels = new ArrayList<>();        
    }

    public int size(){
        return this.lastIdx;
    }

    private String getLabelByIndex(int columnIndex) throws SQLException{
        columnIndex = columnIndex -1;
        if( labels.size() < columnIndex ) throw new SQLException(columnIndex +" > "+ labels.size()+ "인덱스의 범위가 벗어납니다.");
        return labels.get(columnIndex);
    }

    // private JSONObject getJSONObjectValue(int columnIndex ) throws SQLException{        
    //     JSONObject row = bindings.getJSONObject(index);                
    //     JSONObject column = row.getJSONObject(getLabelByIndex(columnIndex));
    //     return column;
    // }

    private JSONObject getJSONObjectValue(String label ) throws SQLException{
        if( !labels.contains(label) ) return null;                 
        JSONObject row = bindings.getJSONObject(index);                
        if(!row.has(label)){
            return null;
        }
        JSONObject column = row.getJSONObject(label);
        return column;
    }


    @Override
    public Object getObject(String columnLabel) throws SQLException {                
        JSONObject obj = getJSONObjectValue(columnLabel);
        if( obj == null ) return null;
        String type = obj.getString("type");
        // System.out.println( "TYPE "+ type );
        if( type.equals("literal") ){
            String value = obj.getString("value");
            LiteralObj data = new LiteralObj(value);
            return data;
        }else if( type.equals("uri") ){
            String uri = obj.getString("value");
            ResourceObj resource = new ResourceObj(uri);
            return resource;
        }
        return obj.get("value");
    }


    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return getObject ( getLabelByIndex(columnIndex) );
    }


    // private Object getObjectValue(int columnIndex) throws SQLException {
    //     return getObjectValue( getLabelByIndex(columnIndex) );        
    // }

    private Object getObjectValue(String columnName) throws SQLException {
        Object obj = getObject(columnName);
        if( obj instanceof LiteralObj ){
            return ( (LiteralObj)obj ).getValue();
        }else if( obj instanceof URL ){
            return obj.toString();
        }else{
            return obj;
        }
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

    @Override
    public boolean next() throws SQLException {
        index = index + 1;
        boolean isNext = ( index < lastIdx );        
        return isNext;
    }

    @Override
    public void close() throws SQLException {
        // TODO Auto-generated method stub        
    }

    @Override
    public boolean wasNull() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {         
        return getString( getLabelByIndex(columnIndex));
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof String ){
            return obj.toString();
        }
        return obj.toString();
        // throw new SQLException("'"+obj+"'는 문자열이 아닙니다.");
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {        
        return getBoolean( getLabelByIndex(columnIndex));        
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Boolean ){
            return (Boolean)obj;
        }
        return Boolean.parseBoolean(obj.toString());
        //throw new SQLException("'"+obj+"'는 Boolean이 아닙니다.");
    }


    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return getByte( getLabelByIndex(columnIndex));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {        
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Byte ){
            return (Byte)obj;
        }
        return Byte.parseByte(obj.toString());
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {        
        return getShort( getLabelByIndex(columnIndex));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Short ){
            return (Short)obj;
        }
        return Short.parseShort(obj.toString());        
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {     
        return getInt( getLabelByIndex(columnIndex));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {        
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Integer ){
            return (Integer)obj;
        }
        return Integer.parseInt(obj.toString());        
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return getLong( getLabelByIndex(columnIndex));        
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Long ){
            return (Long)obj;
        }
        return Long.parseLong(obj.toString());
        // throw new SQLException("'"+obj+"'는 Long이 아닙니다.");
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return getFloat( getLabelByIndex(columnIndex));        
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Float ){
            return (Float)obj;
        }
        return Float.parseFloat(obj.toString());
        // throw new SQLException("'"+obj+"'는 Float이 아닙니다.");
    }

    

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return getDouble( getLabelByIndex(columnIndex));        
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Double ){
            return (Double)obj;
        }
        return Double.parseDouble(obj.toString());
        //throw new SQLException("'"+obj+"'는 Double이 아닙니다.");
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return getBigDecimal( getLabelByIndex(columnIndex));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return getBigDecimal( columnLabel );        
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return getBigDecimal( getLabelByIndex(columnIndex));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof BigDecimal ){
            return (BigDecimal)obj;
        }        
        return new BigDecimal(obj.toString());
        // throw new SQLException("'"+obj+"'는 BigDecimal이 아닙니다.");
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        return getBytes( getLabelByIndex(columnIndex));        
    }


    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);        
        return obj.toString().getBytes();
        // throw new SQLException("'"+obj+"'는 문자열이 아닙니다.");
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        String label = getLabelByIndex(columnIndex);                
        return getDate(label);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Date ){
            return (Date)obj;
        }
        Date sqlDate = Date.valueOf(obj.toString());
        return sqlDate;
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        String label = getLabelByIndex(columnIndex);
        return getTime(label);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Time ){
            return (Time)obj;
        }
        Time sqlTime = Time.valueOf(obj.toString());
        return sqlTime;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        String label = getLabelByIndex(columnIndex);
        return getTimestamp(label);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if(obj instanceof Timestamp ){
            return (Timestamp)obj;
        }
        Timestamp sqlTimeSTimestamp = Timestamp.valueOf(obj.toString());
        return sqlTimeSTimestamp;
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }    

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearWarnings() throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public String getCursorName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new WebSparqlResultSetMetaData(labels);
    }
    

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return labels.indexOf(columnLabel);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }
    

    @Override
    public boolean isBeforeFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFirst() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLast() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeFirst() throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void afterLast() throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public boolean first() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean last() throws SQLException {        
        return (index == lastIdx);
    }

    @Override
    public int getRow() throws SQLException {
        return index;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean previous() throws SQLException {
        index = index - 1;        
        return (index >= 0 && index < lastIdx);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public int getFetchDirection() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getFetchSize() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getType() throws SQLException {
        return 0;
    }

    @Override
    public int getConcurrency() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void insertRow() throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateRow() throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void refreshRow() throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public Statement getStatement() throws SQLException {        
        return stmt;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public int getHoldability() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException();        
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
