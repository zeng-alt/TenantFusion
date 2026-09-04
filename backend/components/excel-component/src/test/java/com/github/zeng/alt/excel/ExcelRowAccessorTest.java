package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.support.ExcelFieldMeta;
import com.github.zeng.alt.excel.support.ExcelRowAccessor;
import lombok.Getter;
import lombok.Setter;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ExcelRowAccessor} 的元数据解析与缓存测试。
 * <p>
 * 它承担的职责是「反射只做一次」——native image 下的实体绑定全靠它，
 * 所以列顺序、字段过滤、缓存复用都得钉住。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelRowAccessorTest {

    @Test
    void 同一类型复用同一个访问器实例() {
        assertThat(ExcelRowAccessor.of(OrderedRow.class)).isSameAs(ExcelRowAccessor.of(OrderedRow.class));
    }

    @Test
    void 列顺序按index再order再声明顺序() {
        List<String> columns = ExcelRowAccessor.of(OrderedRow.class).getFields()
                .stream()
                .map(ExcelFieldMeta::fieldName)
                .toList();

        // second 有 index=0 所以排最前；third/first 无 index，按 order 排
        assertThat(columns).containsExactly("second", "third", "first");
    }

    @Test
    void ExcelIgnore与final静态字段不参与绑定() {
        List<String> columns = ExcelRowAccessor.of(OrderedRow.class).getFields()
                .stream()
                .map(ExcelFieldMeta::fieldName)
                .toList();

        assertThat(columns).doesNotContain("ignored", "CONSTANT");
    }

    @Test
    void 表头保留ExcelProperty原文而不做i18n解析() {
        // i18n 替换交给 I18nHeadWriteHandler，与 engine 绑定路径共用一套逻辑
        ExcelRowAccessor<UserRow> accessor = ExcelRowAccessor.of(UserRow.class);

        assertThat(accessor.head(accessor.getFields()))
                .containsExactly(List.of("{excel.test.userName}"), List.of("{excel.test.age}"));
    }

    @Test
    void include优先于exclude() {
        ExcelRowAccessor<OrderedRow> accessor = ExcelRowAccessor.of(OrderedRow.class);

        assertThat(accessor.selectFields(List.of("first"), List.of("first")))
                .extracting(ExcelFieldMeta::fieldName)
                .containsExactly("first");
        assertThat(accessor.selectFields(null, List.of("first")))
                .extracting(ExcelFieldMeta::fieldName)
                .containsExactly("second", "third");
    }

    @Test
    void 按字段取值与赋值都走缓存好的方法句柄() {
        ExcelRowAccessor<OrderedRow> accessor = ExcelRowAccessor.of(OrderedRow.class);
        List<ExcelFieldMeta> fields = accessor.selectFields(List.of("first"), null);

        OrderedRow row = accessor.instantiate();
        accessor.write(row, fields.getFirst(), "写进去了");

        assertThat(row.getFirst()).isEqualTo("写进去了");
        assertThat(accessor.extract(row, fields)).containsExactly("写进去了");
    }

    /**
     * 覆盖 index / order / @ExcelIgnore / 常量字段四种情形。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Getter
    @Setter
    static class OrderedRow {

        static final String CONSTANT = "不是列";

        @ExcelProperty(value = "第一", order = 20)
        private String first;

        @ExcelProperty(value = "第二", index = 0)
        private String second;

        @ExcelProperty(value = "第三", order = 10)
        private String third;

        @ExcelIgnore
        private String ignored;
    }
}
