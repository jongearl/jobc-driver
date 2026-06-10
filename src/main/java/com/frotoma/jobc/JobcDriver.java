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
package com.frotoma.jobc;

import com.frotoma.jobc.fuseki.FusekiSparqlDriver;
import com.frotoma.jobc.virt.VirtuosoSparqlDriver;
import com.frotoma.jobc.web.WebSparqlDriver;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 * @author jongearl
 */
public class JobcDriver implements java.sql.Driver{
    
    public static final String DRIVER_URL_STARTWITH = "jobc:";
    
    private static final List<Driver> subDrivers = new ArrayList<>();

    private static int MAJOR_VERSION = 1;
    private static int MINOR_VERSION = 1;

    static {
        try {
          DriverManager.registerDriver(new JobcDriver());
          
          // 2. 관리할 3가지 하위 드라이버 리스트 등록          
          subDrivers.add(new FusekiSparqlDriver());
          subDrivers.add(new WebSparqlDriver());
          subDrivers.add(new VirtuosoSparqlDriver());
          
        } catch (SQLException e) {
          throw new RuntimeException("JOBC 메인 드라이버 등록 실패", e);
        }
      }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        
        if (!acceptsURL(url)) {
            return null; // 내가 처리할 수 없으면 다음 JDBC 드라이버로 패스
        }

        // 3가지 드라이버 중 URL을 처리할 수 있는 적절한 드라이버를 찾아 연결 위임
        for (Driver subDriver : subDrivers) {
            if (subDriver.acceptsURL(url)) {
                return subDriver.connect(url, info);
            }
        }
        throw new SQLException("지원하지 않는 JOBC 하위 URL 형식입니다: " + url);
    }
    
    
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        if (url == null || !url.startsWith(DRIVER_URL_STARTWITH)) {
            return false;
        }

        // 주소 검사 시 서브 드라이버가 매칭되는 게 있을 때만 true를 반환
        for (Driver subDriver : subDrivers) {
            if (subDriver.acceptsURL(url)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
    
    
    
}
