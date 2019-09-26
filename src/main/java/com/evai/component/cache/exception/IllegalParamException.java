package com.evai.component.cache.exception;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class IllegalParamException extends RuntimeException {

    public IllegalParamException(String message) {
        super(message);
    }

    public IllegalParamException(Throwable cause) {
        super(cause);
    }

    public IllegalParamException(String message, Throwable cause) {
        super(message, cause);
    }

}
