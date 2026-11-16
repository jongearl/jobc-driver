package com.frotoma.jobc;

/**
 *
 * @author jongearl
 */
public class JobcDriverException extends RuntimeException{

    public JobcDriverException(Exception ex) {
        super(ex);
    }
    
    /** 원인이 SQLException 이면 그대로(SQLState 유지), 아니면 통신 오류(08S01)로 감싼다. */
    public java.sql.SQLException toSQLException() {
        if( getCause() instanceof java.sql.SQLException ){
            return (java.sql.SQLException) getCause();
        }
        return new java.sql.SQLException(getMessage(), "08S01", this);
    }

    public JobcDriverException(String msg, Exception ex) {
        super(msg, ex);
    }
    
}
