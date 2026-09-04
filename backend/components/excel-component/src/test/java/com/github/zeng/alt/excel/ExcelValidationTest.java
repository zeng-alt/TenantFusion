package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelAutoConfiguration;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.read.ExcelRowError;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * jakarta.validation 的逐行校验，含校验分组与「没有 Bean Validation 时的降级」。
 * <p>
 * {@code jakarta.validation-api} 是本模块的 {@code compileOnly} 依赖，所以既要验证
 * 有它时校验真的生效，也要验证没有它时组件不会因为反射枚举
 * {@code ExcelRowValidator} 的方法而抛 {@code NoClassDefFoundError}。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ExcelAutoConfiguration.class,
                    ValidationAutoConfiguration.class));

    // ==================== 基本校验 ====================

    @Test
    void invalidRowsGoToErrorsInsteadOfThrowing() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = write(excelTemplate, List.of(
                    new UserRow("张三", 18),
                    new UserRow("", 30),
                    new UserRow("王五", 0)));

            ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file))
                    .execute();

            assertThat(result.rows()).extracting(UserRow::getUserName).containsExactly("张三");
            assertThat(result.errors()).hasSize(2);
            assertThat(result.errors().getFirst().describe()).contains("姓名不能为空");
            assertThat(result.errors().getLast().describe()).contains("年龄必须大于 0");
        });
    }

    @Test
    void errorCarriesEverythingTheFrontendNeeds() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = write(excelTemplate, List.of(new UserRow("", 18)));

            ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file))
                    .execute();

            ExcelRowError error = result.errors().getFirst();
            // 表头占 1 行，所以第一条数据是用户在 Excel 里看到的第 2 行
            assertThat(error.rowNumber()).isEqualTo(2);
            // 字段能定位到具体列，前端才能高亮出错单元格
            assertThat(error.columnNumber()).isEqualTo(1);
            assertThat(error.field()).isEqualTo("userName");
            assertThat(error.code()).isEqualTo("NotBlank");
            assertThat(error.rejectedValue()).isEmpty();
            assertThat(error.message()).isEqualTo("姓名不能为空");
        });
    }

    @Test
    void oneErrorPerViolatedConstraintNotOnePerRow() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = write(excelTemplate, List.of(new UserRow("", 0)));

            ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file))
                    .execute();

            // 一行两个约束失败 => 两条明细，而不是拼成一条字符串
            assertThat(result.errors()).hasSize(2);
            assertThat(result.errors()).extracting(ExcelRowError::field)
                    .containsExactlyInAnyOrder("age", "userName");
            assertThat(result.errors()).extracting(ExcelRowError::code)
                    .containsExactlyInAnyOrder("Min", "NotBlank");
            // 但只算一行出错
            assertThat(result.summary().errorRows()).isEqualTo(1);
        });
    }

    @Test
    void validationCanBeTurnedOff() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = write(excelTemplate, List.of(new UserRow("", 0)));

            ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                    .from(new ByteArrayInputStream(file))
                    .validate(false)
                    .execute();

            assertThat(result.rows()).hasSize(1);
            assertThat(result.errors()).isEmpty();
        });
    }

    // ==================== 校验分组 ====================

    @Test
    void defaultGroupIgnoresGroupedConstraints() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = writeGrouped(excelTemplate, List.of(new GroupedRow("张三", null)));

            // employeeNo 上的 @NotBlank 属于 OnCreate 分组，默认分组下不该触发
            ExcelReadResult<GroupedRow> result = excelTemplate.read(GroupedRow.class)
                    .from(new ByteArrayInputStream(file))
                    .execute();

            assertThat(result.rows()).hasSize(1);
            assertThat(result.errors()).isEmpty();
        });
    }

    @Test
    void explicitGroupActivatesGroupedConstraints() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = writeGrouped(excelTemplate, List.of(new GroupedRow("张三", null)));

            ExcelReadResult<GroupedRow> result = excelTemplate.read(GroupedRow.class)
                    .from(new ByteArrayInputStream(file))
                    .validationGroups(OnCreate.class)
                    .execute();

            assertThat(result.rows()).isEmpty();
            assertThat(result.errors()).hasSize(1);
            assertThat(result.errors().getFirst().describe()).contains("工号不能为空");
        });
    }

    @Test
    void groupsImplicitlyEnableValidation() {
        runner.run(context -> {
            ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
            byte[] file = writeGrouped(excelTemplate, List.of(new GroupedRow("张三", null)));

            // 先关掉校验，再指定分组——指定分组即视为要校验
            ExcelReadResult<GroupedRow> result = excelTemplate.read(GroupedRow.class)
                    .from(new ByteArrayInputStream(file))
                    .validate(false)
                    .validationGroups(OnCreate.class)
                    .execute();

            assertThat(result.errors()).hasSize(1);
        });
    }

    @Test
    void rowValidatorSupportsGroupsDirectly() {
        runner.run(context -> {
            ExcelRowValidator validator = context.getBean(ExcelRowValidator.class);
            GroupedRow row = new GroupedRow("张三", null);

            assertThat(validator.validate(row)).isEmpty();
            assertThat(validator.validate(row, OnCreate.class)).hasSize(1);
        });
    }

    // ==================== 没有 Bean Validation 时 ====================

    @Test
    void contextStartsWithoutBeanValidation() {
        runner.withClassLoader(new FilteredClassLoader(Validator.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ExcelTemplate.class);
                    assertThat(context).doesNotHaveBean(ExcelRowValidator.class);
                });
    }

    @Test
    void readAndWriteStillWorkWithoutBeanValidation() {
        runner.withClassLoader(new FilteredClassLoader(Validator.class))
                .run(context -> {
                    ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
                    byte[] file = write(excelTemplate, List.of(new UserRow("", 0)));

                    // 没有 Validator，坏行照常收下而不是报错
                    ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(file))
                            .execute();

                    assertThat(result.rows()).hasSize(1);
                    assertThat(result.errors()).isEmpty();
                });
    }

    @Test
    void validationGroupsAreInertWithoutBeanValidation() {
        runner.withClassLoader(new FilteredClassLoader(Validator.class))
                .run(context -> {
                    ExcelTemplate excelTemplate = context.getBean(ExcelTemplate.class);
                    byte[] file = write(excelTemplate, List.of(new UserRow("", 0)));

                    // 指定分组也不该炸，只是没人执行校验
                    ExcelReadResult<UserRow> result = excelTemplate.read(UserRow.class)
                            .from(new ByteArrayInputStream(file))
                            .validationGroups(OnCreate.class)
                            .execute();

                    assertThat(result.rows()).hasSize(1);
                });
    }

    // ==================== 辅助 ====================

    private static byte[] write(ExcelTemplate excelTemplate, List<UserRow> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(UserRow.class).to(output).i18nHead(false).write(rows).get();
        return output.toByteArray();
    }

    private static byte[] writeGrouped(ExcelTemplate excelTemplate, List<GroupedRow> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(GroupedRow.class).to(output).i18nHead(false).write(rows).get();
        return output.toByteArray();
    }

    /** 「新增导入」分组 */
    interface OnCreate {
    }

    /**
     * 带分组约束的测试实体：{@code employeeNo} 只在 {@link OnCreate} 分组下必填。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupedRow {

        @NotBlank(message = "姓名不能为空")
        @ExcelProperty("姓名")
        private String userName;

        @NotBlank(message = "工号不能为空", groups = OnCreate.class)
        @ExcelProperty("工号")
        private String employeeNo;
    }
}
