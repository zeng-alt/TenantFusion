package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelBindingMode;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.fesod.FesodExcelContext;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.fesod.sheet.annotation.ExcelIgnore;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 不可变行模型的构造、转换与校验。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelRecordImportTest {

    private final ValidatorFactory validators = Validation.buildDefaultValidatorFactory();
    private final ExcelTemplate template = new FesodExcelTemplate(new FesodExcelContext(
            new ExcelProperties(),
            TestObjectProviders.empty(),
            TestObjectProviders.empty(),
            TestObjectProviders.of(new ExcelRowValidator(validators.getValidator())),
            TestObjectProviders.of(new DefaultFormattingConversionService())
    ));

    @AfterEach
    void closeValidator() {
        validators.close();
    }

    @ParameterizedTest
    @EnumSource(ExcelBindingMode.class)
    void readsRecordsInConstructorOrderRegardlessOfColumnOrder(ExcelBindingMode binding) {
        final byte[] workbook = workbook(List.of(List.of(20, "Alice")));
        final ExcelReadResult<UserDto> result = template.read(UserDto.class).binding(binding)
                .from(new ByteArrayInputStream(workbook)).execute();

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).containsExactly(new UserDto("Alice", 20, null));
    }

    @Test
    void validatesConstructedRecordsAndCountsConversionFailures() {
        final byte[] workbook = workbook(List.of(List.of(0, "Alice"), List.of("invalid", "Bob"), List.of(25, "Carol")));
        final ExcelReadResult<UserDto> result = template.read(UserDto.class)
                .from(new ByteArrayInputStream(workbook)).execute();

        assertThat(result.rows()).containsExactly(new UserDto("Carol", 25, null));
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().getFirst().field()).isEqualTo("age");
        assertThat(result.summary().totalRows()).isEqualTo(3);
        assertThat(result.summary().errorRows()).isEqualTo(2);
    }

    @Test
    void blankPrimitiveUsesJavaDefaultAndCanBeValidated() {
        final ExcelReadResult<UserDto> result = template.read(UserDto.class)
                .from(new ByteArrayInputStream(workbook(List.of(List.of("", "Alice")))))
                .validate(false).execute();

        assertThat(result.rows()).containsExactly(new UserDto("Alice", 0, null));
    }

    @Test
    void roundTripsRecordsWithoutSetters() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        template.write(UserDto.class).to(output).i18nHead(false)
                .write(List.of(new UserDto("Alice", 20, "not exported"))).get();

        assertThat(template.read(UserDto.class).from(new ByteArrayInputStream(output.toByteArray())).execute().rows())
                .containsExactly(new UserDto("Alice", 20, null));
    }

    @Test
    void supportsFixedIndexesWithDifferentHeadersAndReportsConstructorFailures() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        template.writeHead(List.of(List.of("自定义标题"))).to(output)
                .write(List.of(List.<Object>of("valid"), List.<Object>of("x"), List.<Object>of("invalid"))).get();

        final ExcelReadResult<IndexedDto> result = template.read(IndexedDto.class)
                .from(new ByteArrayInputStream(output.toByteArray())).execute();

        assertThat(result.rows()).containsExactly(new IndexedDto("valid"));
        assertThat(result.errors()).hasSize(2);
        assertThat(result.errors().getFirst().columnNumber()).isEqualTo(1);
        assertThat(result.errors().getLast().message()).contains("名称不允许为 invalid");
    }

    @Test
    void preservesSparseColumnIndexesWhenWritingRecordTemplates() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        template.write(SparseDto.class).to(output).i18nHead(false).write(List.of(new SparseDto("Alice", 20))).get();

        final ExcelReadResult<SparseDto> result = template.read(SparseDto.class)
                .from(new ByteArrayInputStream(output.toByteArray())).execute();

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).containsExactly(new SparseDto("Alice", 20));
    }

    /**
     * 中间留空列的模板模型。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    record SparseDto(
            @ExcelProperty(index = 2) String name,
            @ExcelProperty(index = 0) int age
    ) {
    }

    /**
     * 固定列与规范构造器约束的测试模型。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    record IndexedDto(
            @ExcelProperty(index = 0) @Size(min = 3) String name
    ) {
        IndexedDto {
            if ("invalid".equals(name)) {
                throw new IllegalArgumentException("名称不允许为 invalid");
            }
        }
    }

    private byte[] workbook(List<List<Object>> rows) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        template.writeHead(List.of(List.of("年龄"), List.of("姓名"))).to(output).write(rows).get();
        return output.toByteArray();
    }

    /**
     * 测试不可变行模型。
     *
     * @author zeng-alt
     * @since 0.0.1-SNAPSHOT
     */
    record UserDto(
            @ExcelProperty(value = "姓名", order = 1) @NotBlank String name,
            @ExcelProperty(value = "年龄", order = 0) @Min(1) int age,
            @ExcelIgnore String ignored
    ) {
    }
}
