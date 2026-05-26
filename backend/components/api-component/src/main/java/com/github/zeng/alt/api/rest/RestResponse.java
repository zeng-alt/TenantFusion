package com.github.zeng.alt.api.rest;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author zengJiaJun
 * @crateTime 2024年06月26日 20:19
 * @version 1.0
 */
@Getter
@Setter
public class RestResponse<T> implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public static final Integer SUCCESS_CODE = 200;
	public static final Integer WARN_CODE = 601;
	public static final Integer FAIL_CODE = 500;

	private Integer code;
	private String message;
	private List<Object> error;
	private T data;
	private LocalDateTime time = LocalDateTime.now();

	protected RestResponse() {
	}

	protected RestResponse(Integer code, String message) {
		this.code = code;
		this.message = message;
	}

	public RestResponse<T> error(List<Object> error) {
		this.error = error;
		return this;
	}

	public RestResponse<T> code(Integer code) {
		this.code = code;
		return this;
	}

	public static <T extends Serializable> RestResponse<T> success() {
		return new RestResponse<T>().code(SUCCESS_CODE).message("success");
	}

	public static <T extends Serializable> RestResponse<T> success(T data) {
		return new RestResponse<T>().code(SUCCESS_CODE).message("success").data(data);
	}

	public static RestResponse<Void> fail() {
		return new RestResponse<Void>().code(FAIL_CODE).message("fail").error(new ArrayList<>());
	}

	public static RestResponse<Void> fail(String message) {
		return new RestResponse<Void>().code(FAIL_CODE).message(message).data(null).error(new ArrayList<>());
	}

	public static <T extends Serializable> RestResponse<T> warn() {
		return new RestResponse<T>().code(WARN_CODE).message("warn").data(null).error(new ArrayList<>());
	}

	public static <T extends Serializable> RestResponse<T> warn(String message) {
		return new RestResponse<T>().code(WARN_CODE).message(message).data(null);
	}

	public static <T extends Serializable> RestResponse<T> warn(T data) {
		return new RestResponse<T>().code(WARN_CODE).message("warn").data(data);
	}

	public RestResponse<T> addError(Object error) {
		if (this.error == null) {
			this.error = new ArrayList<>();
		}
		this.error.add(error);
		return this;
	}

	public RestResponse<T> message(String message) {
		this.message = message;
		return this;
	}

	public RestResponse<T> data(T data) {
		this.data = data;
		return this;
	}

	public boolean isSuccess() {
		return Objects.equals(SUCCESS_CODE, code);
	}
}
