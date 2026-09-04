package com.github.zeng.alt.excel.config;

import com.github.zeng.alt.excel.annotation.ExcelExport;
import com.github.zeng.alt.excel.annotation.ExcelImport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 构建期扫出 {@code @ExcelImport} / {@code @ExcelExport} 用到的行类型，注册反射 hints。
 * <p>
 * <b>为什么必须有它。</b>reflective 绑定（native image 下的唯一可用绑定方式，见
 * {@link ExcelBindingMode}）靠反射读行类型的字段、注解、getter/setter。native image
 * 默认把这些元数据全部裁掉，不登记就会在运行时抛
 * {@code NoSuchMethodException}。业务实体是下游模块的类，组件不可能写死，
 * 所以从注解用法反推：controller 方法上的 {@code @ExcelExport} 返回值泛型、
 * 参数上的 {@code @ExcelImport} 泛型，就是要登记的行类型。
 * <p>
 * 覆盖不到的场景——直接调 {@code excelTemplate.read(Xxx.class)} 而没有经过注解——
 * 需要自己写 {@code RuntimeHintsRegistrar}，或给那个类加
 * {@code @RegisterReflectionForBinding}。README 的 native 章节有说明。
 *
 * @author zengJiaJun
 * @since 2026年09月04日
 * @version 1.0
 */
public class ExcelModelAotProcessor implements BeanFactoryInitializationAotProcessor {

    private static final Log LOG = LogFactory.getLog(ExcelModelAotProcessor.class);

    /**
     * 复用 Spring 给「反射绑定的 DTO」准备的注册器（{@code @RegisterReflectionForBinding}
     * 背后就是它）：会把构造器、属性的 getter/setter、字段以及嵌套类型一并登记，
     * 比自己列 {@code MemberCategory} 更完整、也跟着 Spring 版本演进。
     */
    private static final BindingReflectionHintsRegistrar BINDING_REGISTRAR = new BindingReflectionHintsRegistrar();

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        Set<Class<?>> rowTypes = discoverRowTypes(beanFactory);
        if (rowTypes.isEmpty()) {
            return null;
        }
        LOG.info("为 " + rowTypes.size() + " 个 Excel 行类型注册 native 反射 hints: " + rowTypes);
        return new RowTypeHintsContribution(rowTypes);
    }

    private static Set<Class<?>> discoverRowTypes(ConfigurableListableBeanFactory beanFactory) {
        Set<Class<?>> rowTypes = new LinkedHashSet<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = beanFactory.getType(beanName);
            if (beanType != null) {
                collectFrom(ClassUtils.getUserClass(beanType), rowTypes);
            }
        }
        return rowTypes;
    }

    private static void collectFrom(Class<?> beanType, Set<Class<?>> rowTypes) {
        ReflectionUtils.doWithMethods(beanType, method -> {
            collectExport(method, rowTypes);
            collectImports(method, rowTypes);
        }, ReflectionUtils.USER_DECLARED_METHODS);
    }

    private static void collectExport(Method method, Set<Class<?>> rowTypes) {
        ExcelExport annotation = method.getAnnotation(ExcelExport.class);
        if (annotation == null) {
            return;
        }
        if (annotation.type() != Object.class) {
            rowTypes.add(annotation.type());
            return;
        }
        add(rowTypes, ResolvableType.forMethodReturnType(method).getGeneric(0).resolve());
    }

    private static void collectImports(Method method, Set<Class<?>> rowTypes) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getAnnotation(ExcelImport.class) != null) {
                add(rowTypes, ResolvableType.forMethodParameter(new MethodParameter(method, i))
                        .getGeneric(0).resolve());
            }
        }
    }

    private static void add(Set<Class<?>> rowTypes, Class<?> candidate) {
        // 过滤掉 Object 与 JDK 自带类型：它们要么推断失败、要么本来就可达
        if (candidate != null && candidate != Object.class && !candidate.getName().startsWith("java.")) {
            rowTypes.add(candidate);
        }
    }

    /**
     * 把行类型写进 AOT 的 hints 里。
     *
     * @author zengJiaJun
     * @since 2026年09月04日
     * @version 1.0
     */
    private record RowTypeHintsContribution(Set<Class<?>> rowTypes)
            implements BeanFactoryInitializationAotContribution {

        @Override
        public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode code) {
            BINDING_REGISTRAR.registerReflectionHints(
                    generationContext.getRuntimeHints().reflection(), rowTypes.toArray(Class<?>[]::new));
        }
    }
}
