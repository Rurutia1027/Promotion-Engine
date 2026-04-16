package com.tus.coupon.common.web;

import com.tus.coupon.common.errorcode.BaseErrorCode;
import com.tus.coupon.common.exception.AbstractException;

import java.util.Optional;

public final class Results {

    public static Result<Void> success() {
        return new Result<Void>()
                .setCode(Result.SUCCESS_CODE);
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(Result.SUCCESS_CODE)
                .setData(data);
    }

    public static Result<Void> failure() {
        return new Result<Void>()
                .setCode(BaseErrorCode.SERVICE_ERROR.code())
                .setMessage(BaseErrorCode.SERVICE_ERROR.message());
    }


    protected static Result<Void> failure(AbstractException abstractException) {
        String errorCode = Optional.ofNullable(abstractException.getErrorCode())
                .orElse(BaseErrorCode.SERVICE_ERROR.code());
        String errorMessage = Optional.ofNullable(abstractException.getErrorMessage())
                .orElse(BaseErrorCode.SERVICE_ERROR.message());
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }

    protected static Result<Void> failure(String errorCode, String errorMessage) {
        return new Result<Void>()
                .setCode(errorCode)
                .setMessage(errorMessage);
    }
}
