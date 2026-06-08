/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.frotoma.jobc;

import java.io.IOException;

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
