package com.github.zeng.alt.api.rest;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年06月26日 19:30
 */
@Getter
@Setter
public class PageRestResponse<T extends Serializable> extends RestResponse<Collection<T>> {

	private Integer pageNum;

	private Integer pageSize;

	private Long total;

	/**
	 * @return 当前页第一条数据的偏移量（offset）
	 */
	public Integer getFirstNum() {
		return (pageNum - 1) * pageSize;
	}

	/**
	 * @return 总页数
	 */
	public long getTotalPages() {
		return this.total % this.pageSize == 0 ? this.total / this.pageSize : (this.total / this.pageSize) + 1;
	}

	@Override
	public Collection<T> getData() {
		Collection<T> data = super.getData();
		if (null == data) {
			return Collections.emptyList();
		}
		return data;
	}

	public static <T extends Serializable> PageRestResponse<T> of(int pageSize, int pageNum) {
		PageRestResponse<T> response = new PageRestResponse<>();
		response.setData(Collections.emptyList());
		response.setTotal(0L);
		response.setPageSize(pageSize);
		response.setPageNum(pageNum);
		response.setCode(SUCCESS_CODE);
		return response;
	}

	public static <T extends Serializable> PageRestResponse<T> of(Collection<T> data, long totalCount, int pageSize,
																  int pageNum) {
		PageRestResponse<T> response = new PageRestResponse<>();
		response.setData(data);
		response.setTotal(totalCount);
		response.setPageSize(pageSize);
		response.setPageNum(pageNum);
		response.setCode(SUCCESS_CODE);
		return response;
	}

}
