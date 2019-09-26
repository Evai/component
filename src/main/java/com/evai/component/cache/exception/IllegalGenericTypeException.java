package com.evai.component.cache.exception;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class IllegalGenericTypeException extends RuntimeException {

    public IllegalGenericTypeException(String message) {
        super(message);
    }

    public IllegalGenericTypeException(Throwable cause) {
        super(cause);
    }

    public IllegalGenericTypeException(String message, Throwable cause) {
        super(message, cause);
    }

}
