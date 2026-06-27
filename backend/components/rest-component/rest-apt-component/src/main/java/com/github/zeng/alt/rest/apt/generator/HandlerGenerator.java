package com.github.zeng.alt.rest.apt.generator;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.rest.apt.meta.MethodMeta;
import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Modifier;

import io.vavr.control.Option;
import jakarta.servlet.ServletException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;

/**
 * Handler 生成器 — 使用 JavaPoet 生成 Handler 类
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 1.0
 */
public final class HandlerGenerator {

    private static final ClassName SERVER_REQUEST = ClassName.get(ServerRequest.class);
    private static final ClassName SERVER_RESPONSE = ClassName.get(ServerResponse.class);
    private static final ClassName REST_RESPONSE = ClassName.get(RestResponse.class);
    private static final ClassName PAGE_REST_RESPONSE = ClassName.get(PageRestResponse.class);

    private HandlerGenerator() {}

    public static JavaFile generate(RepositoryMeta meta) {
        TypeSpec.Builder handlerBuilder = TypeSpec.classBuilder(meta.getHandlerSimpleName())
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", "rest-apt-component")
                        .build())
                .addAnnotation(Component.class)
                .addModifiers(Modifier.PUBLIC)
                .addField(ClassName.bestGuess(meta.getRepositorySimpleName()), "repository",
                        Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ClassName.bestGuess(meta.getRepositorySimpleName()), "repository")
                        .addStatement("this.repository = repository")
                        .build());

        for (MethodMeta method : meta.getEnabledMethods()) {
            handlerBuilder.addMethod(buildMethod(meta, method));
        }

        return JavaFile.builder(meta.getGeneratedPackageName(), handlerBuilder.build())
                .indent("    ")
                .skipJavaLangImports(true)
                .build();
    }

    private static MethodSpec buildMethod(RepositoryMeta meta, MethodMeta method) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getMethodName())
                .addModifiers(Modifier.PUBLIC)
                .returns(SERVER_RESPONSE)
                .addParameter(ParameterSpec.builder(SERVER_REQUEST, "request").build());

        switch (method) {
            case LIST -> buildListMethod(meta, methodBuilder);
            case DETAIL -> buildDetailMethod(meta, methodBuilder);
            case CREATE -> buildCreateMethod(meta, methodBuilder);
            case UPDATE -> buildUpdateMethod(meta, methodBuilder);
            case DELETE -> buildDeleteMethod(meta, methodBuilder);
        }

        return methodBuilder.build();
    }

    private static void buildListMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName entityType = meta.getEntityType();
        TypeName pageType = ParameterizedTypeName.get(ClassName.get(Page.class), entityType);
        TypeName pageRestType = ParameterizedTypeName.get(PAGE_REST_RESPONSE, entityType);

        builder.addStatement("int page = Integer.parseInt(request.param($S).orElse($S))", "page", "1")
                .addStatement("int size = Integer.parseInt(request.param($S).orElse($S))", "size", "10")
                .addStatement("$T pageResult = repository.findAll($T.of(page - 1, size))",
                        pageType, PageRequest.class)
                .addStatement("$T response = $T.of(pageResult.getContent(), pageResult.getTotalElements(), size, page)",
                        pageRestType, PAGE_REST_RESPONSE)
                .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body(response)",
                        SERVER_RESPONSE, MediaType.class);
    }

    private static void buildDetailMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName entityType = meta.getEntityType();
        TypeName idType = meta.getIdType();
        TypeName optionType = ParameterizedTypeName.get(ClassName.get(Option.class), entityType);

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idType, ClassName.get(Long.class), "id")
                .addStatement("$T result = repository.findById(id)", optionType)
                .addStatement("return result.map(value -> $T.ok().contentType($T.APPLICATION_JSON).body($T.success(value)))" +
                                ".getOrElse($T.notFound().build())",
                        SERVER_RESPONSE, MediaType.class, REST_RESPONSE, SERVER_RESPONSE);
    }

    private static void buildCreateMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName entityType = meta.getEntityType();

        builder.addStatement("$T entity = request.body($T.class)", entityType, entityType)
                .addStatement("$T saved = repository.save(entity)", entityType)
                .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success(saved))",
                        SERVER_RESPONSE, MediaType.class, REST_RESPONSE)
                .addException(ServletException.class)
                .addException(IOException.class);
    }

    private static void buildUpdateMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName entityType = meta.getEntityType();
        TypeName idType = meta.getIdType();
        TypeName optionType = ParameterizedTypeName.get(ClassName.get(Option.class), entityType);

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idType, ClassName.get(Long.class), "id")
                .addStatement("$T entity = request.body($T.class)", entityType, entityType)
                .addStatement("$T result = repository.findById(id)", optionType)
                .beginControlFlow("if (result.isEmpty())")
                .addStatement("return $T.notFound().build()", SERVER_RESPONSE)
                .endControlFlow()
                .addStatement("repository.save(entity)")
                .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success(entity))",
                        SERVER_RESPONSE, MediaType.class, REST_RESPONSE)
                .addException(ServletException.class)
                .addException(IOException.class);
    }

    private static void buildDeleteMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName idType = meta.getIdType();

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idType, ClassName.get(Long.class), "id")
                .addStatement("repository.deleteById(id)")
                .addStatement("return $T.ok().contentType($T.APPLICATION_JSON).body($T.success())",
                        SERVER_RESPONSE, MediaType.class, REST_RESPONSE);
    }
}