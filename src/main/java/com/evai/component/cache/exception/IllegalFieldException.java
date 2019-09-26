package com.evai.component.cache.exception;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class IllegalFieldException extends RuntimeException {

    public IllegalFieldException(String message) {
        super(message);
    }

    public IllegalFieldException(Throwable cause) {
        super(cause);
    }

    public IllegalFieldException(String message, Throwable cause) {
        super(message, cause);
    }

}
