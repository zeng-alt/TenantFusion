package com.github.zeng.alt.excel;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.annotation.ExcelImport;
import com.github.zeng.alt.excel.config.ExcelModelAotProcessor;
import com.github.zeng.alt.excel.config.ExcelRuntimeHints;
import com.github.zeng.alt.excel.dynamic.DynamicCell;
import com.github.zeng.alt.excel.read.ExcelReadResult;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * native image 可达性注册测试。
 * <p>
 * reflective 绑定靠反射读行类型的字段与 getter/setter，native image 默认会把这些
 * 元数据裁掉。业务实体是下游模块的类，组件写不死，只能从 {@code @ExcelImport} /
 * {@code @ExcelExport} 的用法反推——这里就是钉住「真的推出来了」。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
class ExcelAotHintsTest {

    @Test
    void discoversRowTypeFromExcelExportReturnGeneric() {
        RuntimeHints hints = process(ExportController.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(UserRow.class)).accepts(hints);
    }

    @Test
    void discoversRowTypeFromExcelImportParameterGeneric() {
        RuntimeHints hints = process(ImportController.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(UserRow.class)).accepts(hints);
    }

    @Test
    void prefersExplicitTypeOverGenericInference() {
        RuntimeHints hints = process(ExplicitTypeController.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(UserRow.class)).accepts(hints);
    }

    @Test
    void registersConstructorMethodAndFieldOfRowType() throws Exception {
        RuntimeHints hints = process(ImportController.class);

        // 绑定需要：无参构造器实例化 + setter 赋值 + 字段上读 @ExcelProperty
        assertThat(RuntimeHintsPredicates.reflection()
                .onConstructor(UserRow.class.getDeclaredConstructor()).invoke()).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection()
                .onMethod(UserRow.class, "setUserName").invoke()).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection()
                .onField(UserRow.class, "userName")).accepts(hints);
    }

    @Test
    void contributesNothingWhenAnnotationsAreUnused() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition("plain", new RootBeanDefinition(String.class));

        assertThat(new ExcelModelAotProcessor().processAheadOfTime(beanFactory)).isNull();
    }

    @Test
    void registersComponentOwnReachabilityNeeds() {
        RuntimeHints hints = new RuntimeHints();
        new ExcelRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection().onType(DynamicCell.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.resource().forResource("excel.properties")).accepts(hints);
    }

    private static RuntimeHints process(Class<?> controllerType) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition("controller", new RootBeanDefinition(controllerType));

        BeanFactoryInitializationAotContribution contribution =
                new ExcelModelAotProcessor().processAheadOfTime(beanFactory);
        assertThat(contribution).as("应当扫出行类型并产生贡献").isNotNull();

        RuntimeHints hints = new RuntimeHints();
        GenerationContext generationContext = Mockito.mock(GenerationContext.class);
        Mockito.when(generationContext.getRuntimeHints()).thenReturn(hints);
        contribution.applyTo(generationContext, null);
        return hints;
    }

    /**
     * 返回值泛型带行类型。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @RestController
    static class ExportController {

        @PostMapping("/export")
        @ExcelExport("用户")
        List<UserRow> export() {
            return List.of();
        }
    }

    /**
     * 参数泛型带行类型。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @RestController
    static class ImportController {

        @PostMapping("/import")
        String importUsers(@ExcelImport("file") ExcelReadResult<UserRow> result) {
            return "ok";
        }
    }

    /**
     * 泛型被擦除，靠 {@code type()} 显式声明。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    @RestController
    static class ExplicitTypeController {

        @PostMapping("/export-raw")
        @ExcelExport(fileName = "raw", type = UserRow.class)
        Flowable<?> exportRaw() {
            return Flowable.empty();
        }
    }
}
