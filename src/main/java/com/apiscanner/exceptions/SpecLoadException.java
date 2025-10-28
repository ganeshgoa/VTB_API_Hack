// exceptions/SpecLoadException.java
package com.apiscanner.exceptions;

public class SpecLoadException extends Exception {
    public SpecLoadException(String message) {
        super(message);
    }
    
    public SpecLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}