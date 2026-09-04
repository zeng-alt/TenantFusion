package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.config.ExcelBindingMode;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.fesod.FesodExcelContext;
import com.github.zeng.alt.excel.fesod.FesodExcelTemplate;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import com.github.zeng.alt.excel.rx.RxExcel;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * reflective 绑定的读写往返测试。
 * <p>
 * 这条路径是 native image 下唯一可用的实体读写方式：fesod 自己的实体绑定
 * （读的 {@code ModelBuildEventListener#buildUserModel}、写的
 * {@code ExcelWriteAddExecutor#addJavaObjectToExcel}）用 cglib 在运行期生成字节码，
 * native 不支持。这里在 JVM 上强制 {@link ExcelBindingMode#REFLECTIVE}，
 * 保证那条路径本身是正确的——不然 native 构建出来才发现就晚了。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelReflectiveBindingTest {

    private final ExcelProperties properties = properties(ExcelBindingMode.REFLECTIVE);
    private final ExcelTemplate reflective = template(properties);
    private final ExcelTemplate engine = template(properties(ExcelBindingMode.ENGINE));

    @Test
    void roundTripsWithReflectiveBinding() {
        byte[] bytes = write(reflective, new UserRow("张三", 18), new UserRow("李四", 30));

        ExcelReadResult<UserRow> result = reflective.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .execute();

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.rows()).extracting(UserRow::getUserName).containsExactly("张三", "李四");
        // 字符串 → Integer 走 ConversionService，不是硬塞
        assertThat(result.rows()).extracting(UserRow::getAge).containsExactly(18, 30);
    }

    @Test
    void readsReflectiveOutputWithEngineBinding() {
        // 两种绑定产出的表头与列顺序必须一致，否则模板会在升级前后错位
        byte[] bytes = write(reflective, new UserRow("张三", 18));

        List<UserRow> rows = engine.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .execute()
                .rows();

        assertThat(rows).extracting(UserRow::getUserName).containsExactly("张三");
    }

    @Test
    void readsEngineOutputWithReflectiveBinding() {
        byte[] bytes = write(engine, new UserRow("王五", 44));

        List<UserRow> rows = reflective.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .execute()
                .rows();

        assertThat(rows).extracting(UserRow::getUserName).containsExactly("王五");
        assertThat(rows).extracting(UserRow::getAge).containsExactly(44);
    }

    @Test
    void validatesRowsUnderReflectiveBinding() {
        byte[] bytes = write(reflective, new UserRow("张三", 18), new UserRow("", 30));

        ExcelReadResult<UserRow> result = reflective.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .execute();

        assertThat(result.rows()).hasSize(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().getFirst().message()).contains("姓名不能为空");
    }

    @Test
    void supportsColumnFilterUnderReflectiveBinding() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        reflective.write(UserRow.class)
                .to(output)
                .i18nHead(false)
                .excludeColumns(List.of("age"))
                .write(List.of(new UserRow("张三", 18)))
                .get();

        ExcelReadResult<UserRow> result = reflective.read(UserRow.class)
                .from(new ByteArrayInputStream(output.toByteArray()))
                .validate(false)
                .execute();

        assertThat(result.rows()).extracting(UserRow::getUserName).containsExactly("张三");
        // age 列没导出，读回来自然是 null
        assertThat(result.rows().getFirst().getAge()).isNull();
    }

    @Test
    void exportsFromFlowableUnderReflectiveBinding() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Long written = RxExcel.write(
                        reflective.write(UserRow.class).to(output).i18nHead(false),
                        Flowable.just(new UserRow("A", 1), new UserRow("B", 2), new UserRow("C", 3)))
                .get();

        assertThat(written).isEqualTo(3L);
        assertThat(reflective.read(UserRow.class)
                .from(new ByteArrayInputStream(output.toByteArray()))
                .execute()
                .rows()).hasSize(3);
    }

    @Test
    void overridesBindingModePerCall() {
        // 全局配成 REFLECTIVE，某次调用仍能强制 ENGINE（例如该实体依赖自定义 Converter）
        byte[] bytes = write(reflective, new UserRow("张三", 18));

        List<UserRow> rows = reflective.read(UserRow.class)
                .from(new ByteArrayInputStream(bytes))
                .binding(ExcelBindingMode.ENGINE)
                .execute()
                .rows();

        assertThat(rows).extracting(UserRow::getUserName).containsExactly("张三");
    }

    private static byte[] write(ExcelTemplate excelTemplate, UserRow... rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        excelTemplate.write(UserRow.class)
                .to(output)
                .i18nHead(false)
                .write(List.of(rows))
                .get();
        return output.toByteArray();
    }

    private static ExcelProperties properties(ExcelBindingMode binding) {
        ExcelProperties properties = new ExcelProperties();
        properties.setBinding(binding);
        return properties;
    }

    private static ExcelTemplate template(ExcelProperties properties) {
        return new FesodExcelTemplate(new FesodExcelContext(
                properties,
                TestObjectProviders.empty(),
                TestObjectProviders.empty(),
                TestObjectProviders.of(new com.github.zeng.alt.excel.support.ExcelRowValidator(
                        Validation.buildDefaultValidatorFactory().getValidator())),
                TestObjectProviders.of(new DefaultFormattingConversionService())));
    }
}
