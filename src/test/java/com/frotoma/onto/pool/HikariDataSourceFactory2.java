package com.frotoma.onto.pool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariDataSourceFactory2 {

    private static DataSource datasource;

    private static String driver;

    public static DataSource getDataSource() {
        if (datasource == null) {
                InputStream is = null;
                Reader reader;
                File file = null;
                String dbpoolConfig = System.getProperty("dbpool.config");
                
                // 실행시 설정한 경우
                if (dbpoolConfig != null) {
                    file = new File(dbpoolConfig);
                }

                try {

                    if (file != null && file.exists()) {
                        // 실행시 설정한 경우
                        System.out.println("DB POOL CONFIG : "+ dbpoolConfig);
                        is = new FileInputStream(file);
                    } else {
                        // classpath에서 읽어오기
                        is = HikariDataSourceFactory2.class.getResourceAsStream("/dbpool.properties");
                    }

                    Properties prop = new Properties();
                    prop.load(is);
                    driver = prop.getProperty("rdf.driver");                                        

                    String url = prop.getProperty("rdf.url");
                    String user = prop.getProperty("rdf.user");
                    String password = prop.getProperty("rdf.password");

                    HikariConfig config = new HikariConfig();
                    config.setDriverClassName(driver);
                    config.setJdbcUrl(url);
                    config.setUsername(user);
                    config.setPassword(password);


                    config.setMaximumPoolSize(2);
                    config.setAutoCommit(false);
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

                    datasource = new HikariDataSource(config);
                    
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("SqlMapClient Load error." + e, e);
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
        }
        return datasource;
    }

    public static String getDriver(){
        return driver;
    }
    
}
