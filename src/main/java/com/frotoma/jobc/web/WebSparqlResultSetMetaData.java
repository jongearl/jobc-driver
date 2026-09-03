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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class WebSparqlResultSetMetaData implements ResultSetMetaData{

    private List<String> labels = null;

    WebSparqlResultSetMetaData(List<String> labels){
        this.labels = labels == null ? Collections.emptyList() : labels;
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
    public int getColumnCount() throws SQLException {        
        return labels.size();
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return getLabel(column);
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        return getLabel(column);
    }

    private String getLabel(int column) throws SQLException {
        if( column < 1 || column > labels.size() ){
            throw new SQLException("Invalid column index: " + column);
        }
        return labels.get(column - 1);
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getScale(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }
    
}
