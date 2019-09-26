package com.evai.component.cache.exception;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class IllegalAnnotationException extends RuntimeException {

    public IllegalAnnotationException(String message) {
        super(message);
    }

    public IllegalAnnotationException(Throwable cause) {
        super(cause);
    }

    public IllegalAnnotationException(String message, Throwable cause) {
        super(message, cause);
    }

}
