package com.clda.feign.core;

/**
 * Feign调用异常
 * @author ruoyi-xiaozhi
 */
public class FeignCallException extends RuntimeException {

    public FeignCallException(String message) {
        super("feign error: " + message);
    }

}
