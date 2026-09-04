package com.github.zeng.alt.excel.listener;

import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年03月07日 15:29
 */
@Data
public class Head {
    @NonNull
    private final String fieldName;
    @NonNull
    private final Method writeMethod;
    private final Class<?> propertyType;
    private ExcelContentProperty excelContentProperty;
    private Converter<?> converter;

    public Head(@NonNull Method writeMethod, Class<?> propertyType, @NonNull String fieldName) {
        this.writeMethod = writeMethod;
        this.propertyType = propertyType;
        this.fieldName = fieldName;
    }
}
