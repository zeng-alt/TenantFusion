package com.github.zeng.alt.rest.apt.generator;

import com.github.zeng.alt.rest.apt.meta.MethodMeta;
import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Router 生成器 — 使用 JavaPoet 生成 Router Configuration 类
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 1.0
 */
public final class RouterGenerator {

    private static final ClassName SERVER_RESPONSE = ClassName.get(ServerResponse.class);
    private static final ClassName ROUTER_FUNCTIONS = ClassName.get(RouterFunctions.class);
    private static final ParameterizedTypeName ROUTER_FUNCTION_TYPE =
            ParameterizedTypeName.get(ClassName.get(RouterFunction.class), SERVER_RESPONSE);

    private RouterGenerator() {
    }

    public static JavaFile generate(RepositoryMeta meta) {
        String beanName = meta.getRepositorySimpleNameUncapitalized() + "Route";
        String handlerParamName = meta.getRepositorySimpleNameUncapitalized() + "Handler";

        // Build formatted code string: return RouterFunctions.route().GET(...).POST(...).build()
        StringBuilder codeFormat = new StringBuilder("return $T.route()");
        for (MethodMeta ignored : meta.getEnabledMethods()) {
            codeFormat.append("\n    .$L($S, $L)");
        }
        codeFormat.append("\n    .build()");

        // Build all arguments
        Object[] args = new Object[meta.getEnabledMethods().size() * 3 + 1];
        args[0] = ROUTER_FUNCTIONS;
        int argIndex = 1;
        for (MethodMeta method : meta.getEnabledMethods()) {
            args[argIndex++] = method.getHttpMethod();
            args[argIndex++] = meta.getPath() + method.getRouteSuffix();
            args[argIndex++] = handlerParamName + "::" + method.getMethodName();
        }

        MethodSpec routerMethod = MethodSpec.methodBuilder(beanName)
                .addAnnotation(Bean.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ROUTER_FUNCTION_TYPE)
                .addParameter(ClassName.bestGuess(meta.getHandlerSimpleName()), handlerParamName)
                .addStatement(codeFormat.toString(), args)
                .build();

        TypeSpec routerType = TypeSpec.classBuilder(meta.getRouterSimpleName())
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", "rest-apt-component")
                        .build())
                .addAnnotation(Configuration.class)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(routerMethod)
                .build();

        return JavaFile.builder(meta.getGeneratedPackageName(), routerType)
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }
}
