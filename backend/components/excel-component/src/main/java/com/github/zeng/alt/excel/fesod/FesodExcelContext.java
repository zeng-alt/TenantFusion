package com.github.zeng.alt.excel.fesod;

import com.github.zeng.alt.excel.config.ExcelProperties;
import com.github.zeng.alt.excel.support.ExcelRowValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * fesod 实现共用的协作对象，内部参数对象。
 * <p>
 * 存在的意义是让各段链式配置面的构造器保持在 4 个参数以内。
 * <p>
 * 四个协作者全部用 {@link ObjectProvider} 持有、到用时才解析，这不是风格问题而是
 * 装配必需：{@code ConversionService} 在 Web 应用里由
 * {@code WebMvcAutoConfiguration$EnableWebMvcConfiguration} 提供，而后者又要等本组件
 * 贡献的 {@code WebMvcConfigurer}（里面装着 {@code @ExcelImport} 的参数解析器，
 * 它依赖 {@code ExcelTemplate}）。在本类构造时就去取，会形成
 * {@code excelTemplate → ConversionService → EnableWebMvcConfiguration → excelWebMvcConfigurer
 * → excelImportArgumentResolver → excelTemplate} 的循环，上下文直接启动失败。
 * <p>
 * customizer 用 {@code ObjectProvider} 还有第二个理由：容忍零贡献者、尊重
 * {@code @Order}，且每次调用都重新收集，后注册的贡献者也能生效。
 *
 * @param properties         组件配置
 * @param readCustomizers    读 builder 扩展点
 * @param writeCustomizers   写 builder 扩展点
 * @param rowValidators      行校验器；容器里没有 {@code Validator} 时为空
 * @param conversionServices 国际化表头绑定用的类型转换服务；缺省时退回内置实现
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public record FesodExcelContext(
        ExcelProperties properties,
        ObjectProvider<ExcelReadCustomizer> readCustomizers,
        ObjectProvider<ExcelWriteCustomizer> writeCustomizers,
        ObjectProvider<ExcelRowValidator> rowValidators,
        ObjectProvider<ConversionService> conversionServices) {

    /**
     * 行校验器，取不到时返回 {@code null}（读监听器据此跳过校验）。
     *
     * @return 校验器或 {@code null}
     */
    public ExcelRowValidator validator() {
        return rowValidators.getIfAvailable();
    }

    /**
     * 类型转换服务，容器里没有或有多个时退回 Spring 的默认实现。
     *
     * @return 转换服务，永不为 {@code null}
     */
    public ConversionService conversionService() {
        return conversionServices.getIfUnique(DefaultFormattingConversionService::new);
    }
}
