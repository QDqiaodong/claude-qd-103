package com.grain.depot.dto;

public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
