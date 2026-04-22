package com.djx.autosub.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * 自定义业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public BusinessException(int code, String massage) {
        super(massage);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String massage) {
        super(massage);
        this.code = errorCode.getCode();
    }

    @Serial
    private static final long serialVersionUID = 1L;
}
