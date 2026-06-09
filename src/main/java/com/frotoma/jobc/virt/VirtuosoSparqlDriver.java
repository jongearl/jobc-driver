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
package com.frotoma.jobc.virt;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;

public class VirtuosoSparqlDriver implements java.sql.Driver {
    
    private org.apache.logging.log4j.Logger logger = LogManager.getLogger(VirtuosoSparqlDriver.class);

    // URL 스킴
    private static final String JOBC_PREFIX = "jobc:virtuoso:";
    private static final String VIRTUOSO_PREFIX = "jdbc:virtuoso:";

    static {
        try {
            // 로드만 하면 DriverManager에 자동 등록됨
            //Class.forName("virtuoso.jdbc4.Driver");
            //DriverManager.registerDriver(new virtuoso.jdbc4.Driver());
            
            DriverManager.registerDriver(new VirtuosoSparqlDriver());            
        } catch (Exception e) {
            throw new RuntimeException("[VirtuosoSparqlDriver] 드라이버 등록 실패", e);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {             
        if (!acceptsURL(url)) {
            return null;
        }

        String convertedUrl = convertUrl(url);
        logger.info("[VirtuosoSparqlDriver] Virtuoso Sever : " + convertedUrl);        

        // 진짜 Virtuoso 드라이버에게 변환된 url전달하여 커넥션 획득
        //Connection original = originalVirtuosoDriver.connect(convertedUrl, info);
        Connection original = DriverManager.getConnection(convertedUrl, info);
        
        if (original == null) { return null; }

        return new VirtuosoConnection(original);
    }
// jdbc:virt://localhost:1111 → jdbc:virtuoso://localhost:1111
    private String convertUrl(String url) {
        if (url == null) {
            return null;
        }
        
        if (url.startsWith(JOBC_PREFIX)) {
            String converted = VIRTUOSO_PREFIX + url.substring(JOBC_PREFIX.length());
            //logger.info("[VirtuosoSparqlDriver] URL 변환: " + url + " → " + converted);
            return converted;
        }
        return url;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        // jdbc:virt: 로 시작하는 URL 만 처리
        return url != null && url.startsWith(JOBC_PREFIX);
    }

// 나머지 메서드는 connect() 후에만 의미있으니 기본값 반환
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException {
        Driver driver = DriverManager.getDriver(convertUrl(url));
        return driver.getPropertyInfo(convertUrl(url), info);
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
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
