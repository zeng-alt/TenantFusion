package com.github.zeng.alt.excel.web;

import com.github.zeng.alt.excel.ExcelTemplate;
import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.dynamic.DynamicColumn;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.support.ExcelMessageHelper;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 把注解上的配置落到读写链上，Servlet 与 WebFlux 两条集成共用。
 * <p>
 * 抽出来的理由很直接：两个栈的差别只在「怎么拿到上传流」和「怎么写响应」，
 * 注解怎么翻译成链式配置是同一套，不该复制两遍。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
@RequiredArgsConstructor
public class ExcelWebSpecFactory {

    private final ExcelTemplate excelTemplate;
    private final ExcelProperties properties;

    /**
     * 下载文件名里时间戳的格式，取自 {@code alt.excel.write.file-name-timestamp-pattern}。
     *
     * @return 格式串
     */
    public String fileNameTimestampPattern() {
        return properties.getWrite().getFileNameTimestampPattern();
    }

    /**
     * 按 {@code @ExcelImport} 建读取链（还未绑定数据源）。
     *
     * @param rowType    行类型
     * @param annotation 导入注解
     * @return 读取链
     */
    @SuppressWarnings("unchecked")
    public ExcelReadSpec<?> readSpec(Class<?> rowType, ExcelImport annotation) {
        final ExcelReadSpec<?> spec = annotation.dynamic()
                ? excelTemplate.readDynamic((Class<DynamicColumn<DynamicCell>>) rowType)
                : excelTemplate.read(rowType);
        if (annotation.onError().length > 1) {
            throw new IllegalArgumentException("@ExcelImport.onError 只能指定一种策略");
        }
        if (annotation.validate() >= 0) {
            spec.validate(annotation.validate() == 1);
        }
        spec.validationGroups(annotation.validationGroups());
        if (annotation.onError().length == 1) {
            spec.onError(annotation.onError()[0]);
        }
        resolveI18nHead(annotation.i18nHead()).peek(spec::i18nHead);
        if (annotation.headRowNumber() >= 0) {
            spec.headRowNumber(annotation.headRowNumber());
        }
        return spec;
    }

    /**
     * 按 {@code @ExcelExport} 建写出链（还未绑定输出目标）。
     *
     * @param rowType    行类型
     * @param annotation 导出注解
     * @return 写出链
     */
    @SuppressWarnings("unchecked")
    public ExcelWriteSpec<Object> writeSpec(Class<?> rowType, ExcelExport annotation) {
        ExcelWriteSpec<Object> spec = (ExcelWriteSpec<Object>) excelTemplate.write(rowType);
        if (StringUtils.hasText(annotation.sheetName())) {
            spec.sheet(ExcelMessageHelper.resolve(annotation.sheetName()));
        }
        // 注解上是三态开关：-1 沿用配置默认值，0/1 显式覆盖
        if (annotation.autoWidth() >= 0) {
            spec.autoWidth(annotation.autoWidth() == 1);
        }
        resolveI18nHead(annotation.i18nHead()).peek(spec::i18nHead);
        return spec;
    }

    private static Option<Boolean> resolveI18nHead(boolean[] values) {
        if (values.length > 1) {
            throw new IllegalArgumentException("i18nHead 只能指定一个布尔值");
        }
        return values.length == 0 ? Option.none() : Option.some(values[0]);
    }

    /**
     * 从 {@code @ExcelExport} 取显式声明的行类型。
     *
     * @param annotation 导出注解
     * @return 行类型，未显式声明时为 {@code null}
     */
    public static Class<?> explicitRowType(ExcelExport annotation) {
        return annotation.type() == Object.class ? null : annotation.type();
    }
}
