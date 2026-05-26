package com.github.zeng.alt.api.rest;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author zengJiaJun
 * @crateTime 2024年06月16日 20:19
 * @version 1.0
 */
@Data
public class RestResponse<T> implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public static final Integer SUCCESS_CODE = 200;
	public static final Integer WARN_CODE = 601;
	public static final Integer FAIL_CODE = 500;

	private Integer code;
	private String message;
	private List<Object> error;
	protected T data;
	private LocalDateTime time = LocalDateTime.now();

	protected RestResponse() {
	}

	protected RestResponse(Integer code, String message) {
		this.code = code;
		this.message = message;
	}


	protected RestResponse<T> setError(List<Object> error) {
		this.error = error;
		return this;
	}

	protected RestResponse<T> setData(T data) {
		this.data = data;
		return this;
	}

	protected RestResponse<T> setMessage(String message) {
		this.message = message;
		return this;
	}

	protected RestResponse<T> setCode(Integer code) {
		this.code = code;
		return this;
	}



	public static <T extends Serializable> RestResponse<T> success() {
		return new RestResponse<T>().setCode(SUCCESS_CODE).setMessage("success");
	}

	public static <T extends Serializable> RestResponse<T> success(T data) {
		return new RestResponse<T>().setCode(SUCCESS_CODE).setMessage("success").setData(data);
	}

	public static RestResponse<Void> fail() {
		return new RestResponse<Void>().setCode(FAIL_CODE).setMessage("fail").setError(new ArrayList<>());
	}

	public static RestResponse<Void> fail(String message) {
		return new RestResponse<Void>().setCode(FAIL_CODE).setMessage(message).setData(null).setError(new ArrayList<>());
	}

	public static <T extends Serializable> RestResponse<T> warn() {
		return new RestResponse<T>().setCode(WARN_CODE).setMessage("warn").setData(null).setError(new ArrayList<>());
	}

	public static <T extends Serializable> RestResponse<T> warn(String message) {
		return new RestResponse<T>().setCode(WARN_CODE).setMessage(message).setData(null);
	}

	public static <T extends Serializable> RestResponse<T> warn(T data) {
		return new RestResponse<T>().setCode(WARN_CODE).setMessage("warn").setData(data);
	}

	public RestResponse<T> addError(Object error) {
		this.error.add(error);
		return this;
	}

	public RestResponse<T> message(String message) {
		return new RestResponse<T>().setCode(code).setMessage(message).setData(data);
	}

	public RestResponse<T> code(int code) {
		return new RestResponse<T>().setCode(code).setMessage(message).setData(data);
	}

	public RestResponse<T> data(T data) {
		return new RestResponse<T>().setCode(code).setMessage(message).setData(data);
	}

	public boolean isSuccess() {
		return Objects.equals(RestResponse.SUCCESS_CODE, code);
	}

}
