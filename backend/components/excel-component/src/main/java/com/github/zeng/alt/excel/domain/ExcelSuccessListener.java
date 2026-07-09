package com.github.zeng.alt.excel.domain;


import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.compress.utils.Lists;
import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.event.AnalysisEventListener;
import org.apache.fesod.sheet.read.listener.ReadListener;
import org.springframework.core.log.LogMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年07月05日 20:38
 */
@CommonsLog
public abstract class ExcelSuccessListener<T> extends AnalysisEventListener<T>
		implements ReadListener<T>, ExcelResult<T>, ImportCall<T> {

	protected List<T> list;

	protected List<String> errorList;

	protected Map<Integer, String> headMap;

	protected ExcelSuccessListener() {
		this.list = Lists.newArrayList();
		this.errorList = Lists.newArrayList();
	}

	@Override
	public void invokeHeadMap(java.util.Map<Integer, String> headMap, AnalysisContext context) {
		this.headMap = HashMap.ofAll(headMap);
		log.debug(LogMessage.format("解析到表头数据: %s", headMap));
	}

	@Override
	public String call(Consumer<List<T>> success, Consumer<List<String>> error) {
		if (list.isEmpty()) {
			error.accept(errorList);
			return "解析失败";
		}

		if (errorList.isEmpty()) {
			success.accept(list);
			return "解析成功";
		}

		error.accept(errorList);
		return "解析失败";
	}

	@Override
	public List<String> getErrorList() {
		return errorList;
	}

	@Override
	public List<T> getResult() {
		return list;
	}

}
