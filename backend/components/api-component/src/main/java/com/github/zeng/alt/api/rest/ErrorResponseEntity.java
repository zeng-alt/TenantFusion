package com.github.zeng.alt.api.rest;


import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.web.ErrorResponse;

import java.net.URI;

public class ErrorResponseEntity extends HttpEntityStatus<ProblemDetail> implements ErrorResponse {

    private final ProblemDetail body;
    private final String messageDetailCode;
    @Nullable
    private final Object[] messageDetailArguments;

    public ErrorResponseEntity(HttpStatusCode status) {
        this(status, null);
    }

    public ErrorResponseEntity(HttpStatusCode status, @Nullable Throwable cause) {
        this(status, ProblemDetail.forStatus(status), cause);
    }

    public ErrorResponseEntity(HttpStatusCode status, ProblemDetail body, @Nullable Throwable cause) {
        this(status, body, cause, null, null);
    }

    public ErrorResponseEntity(
            HttpStatusCode status, ProblemDetail body, @Nullable Throwable cause,
            @Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {
        super(body, status.value());
        this.body = body;
        this.messageDetailCode = initMessageDetailCode(messageDetailCode);
        this.messageDetailArguments = messageDetailArguments;
    }

    public ErrorResponseEntity(
            Integer status, ProblemDetail body, @Nullable Throwable cause,
            @Nullable String messageDetailCode, @Nullable Object[] messageDetailArguments) {
        super(body, status);
        this.body = body;
        this.messageDetailCode = initMessageDetailCode(messageDetailCode);
        this.messageDetailArguments = messageDetailArguments;
    }

    public static ErrorResponseEntity of(HttpStatusCode status, String message) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, message);
        return new ErrorResponseEntity(status, body, null, null, null);
    }

    public static ErrorResponseEntity of(HttpStatusCode status, Integer code, String message) {
        ProblemDetail body = ProblemDetail.forStatus(code);
        body.setTitle(message);
        body.setDetail(message);
        return new ErrorResponseEntity(status, body, null, null, null);
    }

    public static ErrorResponseEntity of(Integer code, String message) {
        ProblemDetail body = ProblemDetail.forStatus(code);
        body.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.setDetail(message);
        return new ErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, body, null, null, null);
    }

    public static ErrorResponseEntity of(Integer code, String title,  String message) {
        ProblemDetail body = ProblemDetail.forStatus(code);
        body.setTitle(title);
        body.setDetail(message);
        return new ErrorResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, body, null, null, null);
    }

    public static ErrorResponseEntity of(String message) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static ErrorResponseEntity of() {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
    }

    public static ErrorResponseEntity of(HttpStatusCode status, String message, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, message);
        body.setDetail(detail);
        return new ErrorResponseEntity(status, body, null, null, null);
    }

    public static ErrorResponseEntity of(String message, String detail) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, message, detail);
    }

    private String initMessageDetailCode(@Nullable String messageDetailCode) {
        return (messageDetailCode != null ?
                messageDetailCode : ErrorResponse.getDefaultDetailMessageCode(getClass(), null));
    }

    @Override
    public HttpStatusCode getStatusCode() {
        return HttpStatus.valueOf(this.code);
    }

    public ErrorResponseEntity setType(URI type) {
        this.body.setType(type);
        return this;
    }

    public ErrorResponseEntity setTitle(@Nullable String title) {
        this.body.setTitle(title);
        return this;
    }

    public ErrorResponseEntity setDetail(@Nullable String detail) {
        this.body.setDetail(detail);
        return this;
    }

    public ErrorResponseEntity setInstance(@Nullable URI instance) {
        this.body.setInstance(instance);
        return this;
    }

    @Override
    public final ProblemDetail getBody() {
        return this.body;
    }

    @Override
    public String getDetailMessageCode() {
        return this.messageDetailCode;
    }

    @Override
    @Nullable
    public Object[] getDetailMessageArguments() {
        return this.messageDetailArguments;
    }

    public String getMessage() {
        return "status=" + this.code + ", " + this.body;
    }
}
