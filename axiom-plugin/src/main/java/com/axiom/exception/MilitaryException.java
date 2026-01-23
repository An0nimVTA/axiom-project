package com.axiom.exception;

public class MilitaryException extends RuntimeException {
    public MilitaryException(String message) {
        super(message);
    }
    
    public MilitaryException(String message, Throwable cause) {
        super(message, cause);
    }
}
