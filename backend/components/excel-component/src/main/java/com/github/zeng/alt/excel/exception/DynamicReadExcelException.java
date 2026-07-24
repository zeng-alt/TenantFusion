package com.github.zeng.alt.excel.exception;


import com.github.zeng.alt.api.exception.BaseI18nException;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月10日 21:50
 */
public class DynamicReadExcelException extends BaseI18nException {

    public DynamicReadExcelException(String message) {
        super(message);
    }

    public DynamicReadExcelException(String code, String message) {
        super(code, message);
    }
}
