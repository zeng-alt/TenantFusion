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
 * @since 2024年06月26日 20:19
 * @version 1.0
 */
@Getter
@Setter
public class RestResponse<T> implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public static final Integer SUCCESS_CODE = 200;
	public static final Integer WARN_CODE = 601;
	public static final Integer FAIL_CODE = 600;

	private Integer status;
	private String title;
	private List<String> error = new ArrayList<>();
	private T data;
	private LocalDateTime time = LocalDateTime.now();

	protected RestResponse() {
	}

	protected RestResponse(Integer code, String title) {
		this.status = code;
		this.title = title;
	}

	public RestResponse<T> error(List<String> error) {
		this.error = error;
		return this;
	}

	public RestResponse<T> code(Integer code) {
		this.status = code;
		return this;
	}

	public RestResponse<T> title(String title) {
		this.title = title;
		return this;
	}

	public static <T> RestResponse<T> status(Integer status) {
		return new RestResponse<T>().code(status);
	}

	public static <T> RestResponse<T> success() {
		return new RestResponse<T>().code(SUCCESS_CODE).message("success");
	}

	public static <T> RestResponse<T> success(T data) {
		return new RestResponse<T>().code(SUCCESS_CODE).title("业务错误").message("success").data(data);
	}

	public static <T> RestResponse<T> fail() {
		return new RestResponse<T>().code(FAIL_CODE).title("业务错误").message("fail").error(new ArrayList<>());
	}

	public static <T> RestResponse<T> fail(String message) {
		return new RestResponse<T>().code(FAIL_CODE).title("业务错误").message(message).data(null).error(new ArrayList<>());
	}

	public static <T> RestResponse<T> warn() {
		return new RestResponse<T>().code(WARN_CODE).message("warn").data(null).error(new ArrayList<>());
	}

	public static <T> RestResponse<T> warn(String message) {
		return new RestResponse<T>().code(WARN_CODE).message(message).data(null);
	}

	public static <T> RestResponse<T> warn(T data) {
		return new RestResponse<T>().code(WARN_CODE).message("warn").data(data);
	}

	public RestResponse<T> addError(String error) {
		if (this.error == null) {
			this.error = new ArrayList<>();
		}
		this.error.add(error);
		return this;
	}

	public RestResponse<T> message(String message) {
		this.title = message;
		return this;
	}

	public RestResponse<T> data(T data) {
		this.data = data;
		return this;
	}

	public int getCode() {
		return status;
	}

	public String getDetail() {
		return String.join("\n", error);
	}

	public boolean isSuccess() {
		return Objects.equals(SUCCESS_CODE, status);
	}
}
