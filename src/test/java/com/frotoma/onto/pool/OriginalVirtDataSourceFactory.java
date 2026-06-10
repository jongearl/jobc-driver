package com.frotoma.onto.pool;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class OriginalVirtDataSourceFactory {

    private static DataSource datasource;

    public static DataSource getDataSource() {

        if (datasource == null) {

            try{

//            String url =    "jdbc:http://192.168.10.4:8890/sparql/";
            String url = "jdbc:virtuoso://192.168.10.4:1111/DATABASE=dba/CHARSET=UTF-8";
            String user = "dba";
            String password = "dba";

            HikariConfig config = new HikariConfig();

            config.setDriverClassName("virtuoso.jdbc4.Driver");
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);

            config.setMaximumPoolSize(2);
            config.setAutoCommit(false);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            datasource = new HikariDataSource(config);

            }catch(Exception e){
                e.printStackTrace();
            }

            
        }
        return datasource;
    }
    
}
