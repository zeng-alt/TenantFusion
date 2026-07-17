package com.github.zeng.alt.api.rest;

import lombok.Getter;
import lombok.Setter;


import java.util.Collection;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2024年06月26日 19:30
 */
@Getter
@Setter
public class PageRestResponse<T> extends RestResponse<PageEntity<T>> {




	public static <T> PageRestResponse<T> of(int pageSize, int pageNum) {
		PageRestResponse<T> response = new PageRestResponse<>();
		PageEntity<T> entity = new PageEntity<>();
		entity.setTotal(0L);
		entity.setPageSize(pageSize);
		entity.setPageNum(pageNum);
		response.setData(entity);
		response.code(SUCCESS_CODE);
		return response;
	}

	public static <E> PageRestResponse<E> of(Collection<E> data, long totalCount, int pageSize,
																  int pageNum) {
		return of(data, totalCount, pageSize, pageNum);
	}

	public static <E> PageRestResponse<E> of(List<E> data, long totalCount, int pageSize,
                                                                  int pageNum) {
		PageRestResponse<E> response = new PageRestResponse<>();
		PageEntity<E> entity = new PageEntity<>();
		entity.setTotal(totalCount);
		entity.setPageSize(pageSize);
		entity.setPageNum(pageNum);
		entity.setPageData(data);
		response.setData(entity);
		response.code(SUCCESS_CODE);
		return response;
	}

}
