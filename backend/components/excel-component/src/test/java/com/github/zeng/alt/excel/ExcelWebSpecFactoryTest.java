package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.read.ExcelErrorPolicy;
import com.github.zeng.alt.excel.read.ExcelReadSpec;
import com.github.zeng.alt.excel.web.ExcelWebSpecFactory;
import com.github.zeng.alt.excel.write.ExcelWriteSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 导入注解只覆盖显式声明的配置。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
class ExcelWebSpecFactoryTest {

    private final ExcelTemplate template = mock(ExcelTemplate.class);
    private final ExcelReadSpec<?> spec = mock(ExcelReadSpec.class, RETURNS_SELF);
    private final ExcelWebSpecFactory factory = new ExcelWebSpecFactory(template, new ExcelProperties());

    @Test
    void unspecifiedOptionsPreserveTemplateDefaults() throws Exception {
        when(template.read(UserRow.class)).thenAnswer(invocation -> spec);

        factory.readSpec(UserRow.class, annotation("defaults"));

        verify(spec, never()).validate(anyBoolean());
        verify(spec, never()).i18nHead(anyBoolean());
        verify(spec, never()).onError(any());
    }

    @Test
    void explicitFalseAndSkipRowOverrideGlobalDefaults() throws Exception {
        when(template.read(UserRow.class)).thenAnswer(invocation -> spec);

        factory.readSpec(UserRow.class, annotation("disabled"));

        verify(spec).validate(false);
        verify(spec).i18nHead(false);
        verify(spec).onError(ExcelErrorPolicy.SKIP_ROW);
    }

    @Test
    void explicitTrueAndValidationGroupsAreApplied() throws Exception {
        when(template.read(UserRow.class)).thenAnswer(invocation -> spec);

        factory.readSpec(UserRow.class, annotation("enabled"));

        verify(spec).validate(true);
        verify(spec).i18nHead(true);
        verify(spec).validationGroups(ImportGroup.class);
    }

    @Test
    void multiplePoliciesAreRejectedAtAnnotationBoundary() throws Exception {
        when(template.read(UserRow.class)).thenAnswer(invocation -> spec);
        final ExcelImport annotation = annotation("conflicting");

        assertThatThrownBy(() -> factory.readSpec(UserRow.class, annotation))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("只能指定一种策略");
    }

    @Test
    void unspecifiedExportHeadPreservesTemplateDefault() throws Exception {
        final ExcelWriteSpec<?> writeSpec = mock(ExcelWriteSpec.class, RETURNS_SELF);
        when(template.write(UserRow.class)).thenAnswer(invocation -> writeSpec);

        factory.writeSpec(UserRow.class, exportAnnotation("exportDefaults"));

        verify(writeSpec, never()).i18nHead(anyBoolean());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void explicitExportHeadOverridesTemplateDefault(boolean enabled) throws Exception {
        final ExcelWriteSpec<?> writeSpec = mock(ExcelWriteSpec.class, RETURNS_SELF);
        when(template.write(UserRow.class)).thenAnswer(invocation -> writeSpec);

        factory.writeSpec(UserRow.class, exportAnnotation(enabled ? "exportEnabled" : "exportDisabled"));

        verify(writeSpec).i18nHead(enabled);
    }

    @Test
    void multipleImportHeadValuesAreRejected() throws Exception {
        when(template.read(UserRow.class)).thenAnswer(invocation -> spec);
        final ExcelImport annotation = annotation("conflictingHead");

        assertThatThrownBy(() -> factory.readSpec(UserRow.class, annotation))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("i18nHead 只能指定一个布尔值");
    }

    @Test
    void multipleExportHeadValuesAreRejected() throws Exception {
        when(template.write(UserRow.class)).thenAnswer(invocation -> mock(ExcelWriteSpec.class, RETURNS_SELF));
        final ExcelExport annotation = exportAnnotation("exportConflictingHead");

        assertThatThrownBy(() -> factory.writeSpec(UserRow.class, annotation))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("i18nHead 只能指定一个布尔值");
    }

    private ExcelExport exportAnnotation(String method) throws Exception {
        return getClass().getDeclaredMethod(method).getAnnotation(ExcelExport.class);
    }

    private ExcelImport annotation(String method) throws Exception {
        return getClass().getDeclaredMethod(method, List.class).getParameters()[0].getAnnotation(ExcelImport.class);
    }

    private void defaults(@ExcelImport List<UserRow> rows) {
    }

    private void disabled(
            @ExcelImport(validate = 0, i18nHead = false, onError = ExcelErrorPolicy.SKIP_ROW) List<UserRow> rows
    ) {
    }

    private void enabled(
            @ExcelImport(validate = 1, i18nHead = true, validationGroups = ImportGroup.class) List<UserRow> rows
    ) {
    }

    private void conflicting(
            @ExcelImport(onError = {ExcelErrorPolicy.SKIP_ROW, ExcelErrorPolicy.FAIL_FAST}) List<UserRow> rows
    ) {
    }

    private void conflictingHead(@ExcelImport(i18nHead = {true, false}) List<UserRow> rows) {
    }

    @ExcelExport
    private void exportDefaults() {
    }

    @ExcelExport(i18nHead = true)
    private void exportEnabled() {
    }

    @ExcelExport(i18nHead = false)
    private void exportDisabled() {
    }

    @ExcelExport(i18nHead = {true, false})
    private void exportConflictingHead() {
    }

    private interface ImportGroup {
    }
}
