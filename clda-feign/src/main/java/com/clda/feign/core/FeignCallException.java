package com.clda.feign.core;

/**
 * Feign调用异常
 * @author clda-xiaozhi
 */
public class FeignCallException extends RuntimeException {

    public FeignCallException(String message) {
        super("feign error: " + message);
    }

}
