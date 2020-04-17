package com.sensetime.tsc.streaming.exception;

/**
 * @Author: zhouyuyang
 * @Date: 2020/4/17 10:14
 */
public class BusinessException extends RuntimeException {

    private String code = "500";
    private String message;

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(String errCode, String message) {
        super(message);
        this.code = errCode;
        this.message = message;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode;
        this.message = message;
    }

    public String getCode() {
        return this.code;
    }

}
