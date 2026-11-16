package com.frotoma.jobc.web;

import com.frotoma.jobc.obj.BNodeObj;
import com.frotoma.jobc.obj.LiteralObj;
import com.frotoma.jobc.obj.ResourceObj;
import com.frotoma.jobc.obj.TripleObj;

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
import java.sql.SQLFeatureNotSupportedException;
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
        
        try {
            parse(jsonString);
        } catch (org.json.JSONException e) {
            throw new SQLException("Malformed SPARQL JSON result: " + e.getMessage(), "HY000", e);
        }
        if( this.labels == null ){
            throw new SQLException("Unexpected SPARQL result: no head/results or boolean", "HY000");
        }
    }

    private void parse(String jsonString) throws SQLException {
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

    /** 1부터 시작하는 컬럼 번호 → 변수 이름. 범위 밖이면 SQLState 07009 */
    private String getLabelByIndex(int columnIndex) throws SQLException{
        if( columnIndex < 1 || columnIndex > labels.size() ){
            throw new SQLException("Column index out of range: " + columnIndex + " (columns: " + labels.size() + ")", "07009");
        }
        return labels.get(columnIndex - 1);
    }

    // private JSONObject getJSONObjectValue(int columnIndex ) throws SQLException{        
    //     JSONObject row = bindings.getJSONObject(index);                
    //     JSONObject column = row.getJSONObject(getLabelByIndex(columnIndex));
    //     return column;
    // }

    private JSONObject getJSONObjectValue(String label ) throws SQLException{
        if( !labels.contains(label) ){
            throw new SQLException("Unknown column: " + label, "42S22");
        }
        if( index < 0 || index >= lastIdx ){
            throw new SQLException("No current row", "24000");
        }
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
            String lang = obj.has("xml:lang") ? obj.getString("xml:lang") : null;
            String datatype = obj.has("datatype") ? obj.getString("datatype") : null;
            LiteralObj data = new LiteralObj(value, lang, datatype);
            return data;
        }else if( type.equals("uri") ){
            String uri = obj.getString("value");
            ResourceObj resource = new ResourceObj(uri);
            return resource;
        }else if( type.equals("bnode") ){
            String bnodeString = obj.getString("value");
            BNodeObj bnode = new BNodeObj(bnodeString);
            return bnode;
        }else if( type.equals("triple")){
            TripleObj triple = null;
            JSONObject tripleObj = obj.getJSONObject("value");

            ResourceObj subject = new ResourceObj( tripleObj.getJSONObject("subject").getString("value") );
            ResourceObj predicate = new ResourceObj( tripleObj.getJSONObject("predicate").getString("value") );

            JSONObject objectObj = tripleObj.getJSONObject("object");
            
            if( objectObj.getString("type").equals("uri") ){
                ResourceObj resourceObject = new ResourceObj( objectObj.getString("value") );
                triple = new TripleObj(subject, predicate, resourceObject);
            } else if( objectObj.getString("type").equals("literal") ){
                String literalLang = objectObj.has("xml:lang") ? objectObj.getString("xml:lang") : null;
                String literalDatatype = objectObj.has("datatype") ? objectObj.getString("datatype") : null;
                LiteralObj literalObject = new LiteralObj( objectObj.getString("value"), literalLang, literalDatatype );
                triple = new TripleObj(subject, predicate, literalObject);
            }
            return triple;
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

    /** 마지막으로 읽은 값이 바인딩되지 않았는지 (JDBC wasNull) */
    private boolean lastWasNull = false;

    private Object getObjectValue(String columnName) throws SQLException {
        Object obj = getObject(columnName);
        lastWasNull = ( obj == null );
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
        if( iface.isInstance(this) ){
            return iface.cast(this);
        }
        throw new SQLException("Not a wrapper for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
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
        return lastWasNull;
    }

    /** 숫자형 getter 공통: 값을 BigDecimal 로. 바인딩되지 않았으면 null. 숫자가 아니면 SQLState 22018 */
    private BigDecimal numberValue(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if( obj == null ){
            return null;
        }
        if( obj instanceof BigDecimal ){
            return (BigDecimal) obj;
        }
        if( obj instanceof Boolean ){
            return ((Boolean) obj) ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (NumberFormatException e) {
            throw new SQLException("'" + obj + "' is not a number (column " + columnLabel + ")", "22018", e);
        }
    }

    /** 정수형 getter 공통: 소수점 아래는 버리고, 범위를 넘으면 SQLState 22003 */
    private long integralValue(String columnLabel, long min, long max) throws SQLException {
        BigDecimal bd = numberValue(columnLabel);
        if( bd == null ){
            return 0;
        }
        try {
            long v = bd.setScale(0, java.math.RoundingMode.DOWN).longValueExact();
            if( v < min || v > max ){
                throw new ArithmeticException();
            }
            return v;
        } catch (ArithmeticException e) {
            throw new SQLException("'" + bd + "' is out of range (column " + columnLabel + ")", "22003");
        }
    }

    @Override
    public String getString(int columnIndex) throws SQLException {         
        return getString( getLabelByIndex(columnIndex));
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return lexical(columnLabel);
        // throw new SQLException("'"+obj+"'는 문자열이 아닙니다.");
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {        
        return getBoolean( getLabelByIndex(columnIndex));        
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        Object obj = getObjectValue(columnLabel);
        if( obj == null ){
            return false;
        }
        if(obj instanceof Boolean ){
            return (Boolean)obj;
        }
        String s = obj.toString().trim();
        return s.equalsIgnoreCase("true") || s.equals("1");
        //throw new SQLException("'"+obj+"'는 Boolean이 아닙니다.");
    }


    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return getByte( getLabelByIndex(columnIndex));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {        
        return (byte) integralValue(columnLabel, Byte.MIN_VALUE, Byte.MAX_VALUE);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {        
        return getShort( getLabelByIndex(columnIndex));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return (short) integralValue(columnLabel, Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {     
        return getInt( getLabelByIndex(columnIndex));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {        
        return (int) integralValue(columnLabel, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return getLong( getLabelByIndex(columnIndex));        
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return integralValue(columnLabel, Long.MIN_VALUE, Long.MAX_VALUE);
        // throw new SQLException("'"+obj+"'는 Long이 아닙니다.");
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return getFloat( getLabelByIndex(columnIndex));        
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        BigDecimal bd = numberValue(columnLabel);
        return bd == null ? 0f : bd.floatValue();
        // throw new SQLException("'"+obj+"'는 Float이 아닙니다.");
    }

    

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return getDouble( getLabelByIndex(columnIndex));        
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        BigDecimal bd = numberValue(columnLabel);
        return bd == null ? 0d : bd.doubleValue();
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
        return numberValue(columnLabel);
        // throw new SQLException("'"+obj+"'는 BigDecimal이 아닙니다.");
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        return getBytes( getLabelByIndex(columnIndex));        
    }


    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        String s = lexical(columnLabel);
        return s == null ? null : s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** 저장소가 돌려준 값 그대로(리터럴은 lexical form). 바인딩되지 않았으면 null 이고 wasNull 이 true */
    private String lexical(String columnLabel) throws SQLException {
        Object raw = getObject(columnLabel);
        lastWasNull = ( raw == null );
        if( raw == null ){
            return null;
        }
        if( raw instanceof LiteralObj ){
            String lf = ((LiteralObj) raw).getLexicalForm();
            return lf != null ? lf : String.valueOf(((LiteralObj) raw).getValue());
        }
        return raw.toString();
    }

    private static SQLException badValue(String s, String type, Exception cause){
        return new SQLException("'" + s + "' is not a valid " + type, "22018", cause);
    }

    /** xsd:dateTime / xsd:date 문자열 → Timestamp. 오프셋이 있으면 그 순간을 JVM 시간대로 */
    private static Timestamp toTimestamp(String s) throws SQLException {
        try {
            if( s.indexOf('T') > 0 ){
                try {
                    return Timestamp.from(java.time.OffsetDateTime.parse(s).toInstant());
                } catch (java.time.format.DateTimeParseException noOffset) {
                    return Timestamp.valueOf(java.time.LocalDateTime.parse(s));
                }
            }
            return Timestamp.valueOf(toLocalDate(s).atStartOfDay());
        } catch (RuntimeException e) {
            throw badValue(s, "timestamp", e);
        }
    }

    /** xsd:date("2026-11-16", "2026-11-16+09:00") 또는 xsd:dateTime 의 날짜 부분 */
    private static java.time.LocalDate toLocalDate(String s) throws SQLException {
        try {
            if( s.indexOf('T') > 0 ){
                return toTimestamp(s).toLocalDateTime().toLocalDate();
            }
            return java.time.LocalDate.parse(s.length() > 10 && s.charAt(0) != '-' ? s.substring(0, 10) : s);
        } catch (RuntimeException e) {
            throw badValue(s, "date", e);
        }
    }

    /** xsd:time("10:20:30", 타임존 포함 가능) 또는 xsd:dateTime 의 시간 부분 */
    private static java.time.LocalTime toLocalTime(String s) throws SQLException {
        try {
            if( s.indexOf('T') > 0 ){
                return toTimestamp(s).toLocalDateTime().toLocalTime();
            }
            String t = s.endsWith("Z") ? s.substring(0, s.length() - 1) : s;
            int tz = Math.max(t.lastIndexOf('+'), t.lastIndexOf('-'));
            if( tz > 0 ){
                t = t.substring(0, tz);
            }
            return java.time.LocalTime.parse(t);
        } catch (RuntimeException e) {
            throw badValue(s, "time", e);
        }
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        String label = getLabelByIndex(columnIndex);                
        return getDate(label);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        String s = lexical(columnLabel);
        return s == null ? null : Date.valueOf(toLocalDate(s));
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        String label = getLabelByIndex(columnIndex);
        return getTime(label);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        String s = lexical(columnLabel);
        return s == null ? null : Time.valueOf(toLocalTime(s));
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        String label = getLabelByIndex(columnIndex);
        return getTimestamp(label);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        String s = lexical(columnLabel);
        return s == null ? null : toTimestamp(s);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }    

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        // 경고 없음
    }

    @Override
    public String getCursorName() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return new WebSparqlResultSetMetaData(labels);
    }
    

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        int i = labels.indexOf(columnLabel);
        if( i < 0 ){
            throw new SQLException("Unknown column: " + columnLabel, "42S22");
        }
        return i + 1;
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
    

    @Override
    public boolean isBeforeFirst() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isFirst() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isLast() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void beforeFirst() throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void afterLast() throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public boolean first() throws SQLException {
        throw new SQLFeatureNotSupportedException();
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
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean previous() throws SQLException {
        index = index - 1;        
        return (index >= 0 && index < lastIdx);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public int getFetchDirection() throws SQLException {
        throw new SQLFeatureNotSupportedException();
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
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void insertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void refreshRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
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

    // Calendar 버전: xsd 값은 타임존을 직접 갖거나 로컬 값이므로 Calendar 는 쓰지 않는다
    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return getDate(columnIndex);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return getDate(columnLabel);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return getTime(columnIndex);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return getTimestamp(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return getTimestamp(columnLabel);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        return getURL(getLabelByIndex(columnIndex));
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        String s = lexical(columnLabel);
        if( s == null ){
            return null;
        }
        try {
            return java.net.URI.create(s).toURL();
        } catch (IllegalArgumentException | java.net.MalformedURLException e) {
            throw badValue(s, "URL", e);
        }
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
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
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
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
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException();        
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
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return getString(columnLabel);
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
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException();
        
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return getObject(getLabelByIndex(columnIndex), type);
    }

    /** JDBC 4.1 getObject(col, Class). MyBatis 의 java.time TypeHandler 가 이 메서드를 쓴다. 바인딩되지 않았으면 null */
    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        String s = lexical(columnLabel);
        if( s == null ){
            return null;
        }
        Object v;
        if( type == String.class ) v = s;
        else if( type == Integer.class ) v = getInt(columnLabel);
        else if( type == Long.class ) v = getLong(columnLabel);
        else if( type == Short.class ) v = getShort(columnLabel);
        else if( type == Byte.class ) v = getByte(columnLabel);
        else if( type == Double.class ) v = getDouble(columnLabel);
        else if( type == Float.class ) v = getFloat(columnLabel);
        else if( type == Boolean.class ) v = getBoolean(columnLabel);
        else if( type == BigDecimal.class ) v = getBigDecimal(columnLabel);
        else if( type == java.math.BigInteger.class ) v = getBigDecimal(columnLabel).toBigInteger();
        else if( type == java.time.LocalDate.class ) v = toLocalDate(s);
        else if( type == java.time.LocalTime.class ) v = toLocalTime(s);
        else if( type == java.time.LocalDateTime.class ) v = toTimestamp(s).toLocalDateTime();
        else if( type == java.time.OffsetDateTime.class ) v = toOffsetDateTime(s);
        else if( type == java.time.Instant.class ) v = toTimestamp(s).toInstant();
        else if( type == Timestamp.class || type == java.util.Date.class ) v = toTimestamp(s);
        else if( type == Date.class ) v = Date.valueOf(toLocalDate(s));
        else if( type == Time.class ) v = Time.valueOf(toLocalTime(s));
        else if( type == URL.class ) v = getURL(columnLabel);
        else {
            Object raw = getObject(columnLabel);
            if( type.isInstance(raw) ){
                v = raw;
            }else if( raw instanceof LiteralObj && type.isInstance(((LiteralObj) raw).getValue()) ){
                v = ((LiteralObj) raw).getValue();
            }else{
                throw new SQLFeatureNotSupportedException("Cannot convert column " + columnLabel + " to " + type.getName());
            }
        }
        return type.cast(v);
    }

    private static java.time.OffsetDateTime toOffsetDateTime(String s) throws SQLException {
        try {
            return java.time.OffsetDateTime.parse(s);
        } catch (java.time.format.DateTimeParseException noOffset) {
            return toTimestamp(s).toLocalDateTime().atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();
        }
    }
    
}
