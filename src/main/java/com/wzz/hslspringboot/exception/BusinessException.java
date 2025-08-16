package com.wzz.hslspringboot.exception;

public class BusinessException extends RuntimeException {
    private int code;
    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }
    public int getCode() { return code; }
}