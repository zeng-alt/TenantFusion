package com.github.zeng.alt.excel.domain;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2024年07月23日 17:32
 */
public class ExcelHandlerManger<T> {

	// 原签名引用的 TestHandler 全仓不存在（包重命名遗留的占位类型），
	// 且本方法体是空壳。这里改用本类自身的泛型参数，消除悬空引用。
	public ExcelHandlerManger<T> invoke(InvokeFunction<T> invokeFunction) {
		return this;
	}

	public ExcelHandlerManger<T> success(Consumer<List<T>> consumer) {
		return this;
	}

	public ExcelHandlerManger<T> fail(Consumer<List<String>> consumer) {
		return this;
	}

}
