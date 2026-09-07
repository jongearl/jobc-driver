package com.frotoma.jobc;

/**
 *
 * @author jongearl
 */
public class JobcDriverException extends RuntimeException{

    public JobcDriverException(Exception ex) {
        super(ex);
    }
    
    public JobcDriverException(String msg, Exception ex) {
        super(msg, ex);
    }
    
}
