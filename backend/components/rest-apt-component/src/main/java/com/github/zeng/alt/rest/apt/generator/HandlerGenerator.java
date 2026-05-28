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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Optional;

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

    private HandlerGenerator() {
    }

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
        TypeName entityTypeName = meta.getEntityType();
        TypeName pageRestResponseType = ParameterizedTypeName.get(PAGE_REST_RESPONSE, entityTypeName);
        TypeName pageType = ParameterizedTypeName.get(ClassName.get(Page.class), entityTypeName);

        builder.addStatement("int $L = Integer.parseInt(request.param($S).orElse($S))",
                        "page", "page", "1")
                .addStatement("int $L = Integer.parseInt(request.param($S).orElse($S))",
                        "size", "size", "10")
                .addStatement("$T pageResult = repository.findAll($T.of(page - 1, size))",
                        pageType, PageRequest.class)
                .addStatement("$T response = $T.of(pageResult.getContent(), " +
                                "pageResult.getTotalElements(), size, page)",
                        pageRestResponseType, PAGE_REST_RESPONSE)
                .addStatement("return $T.ok().body(response)", SERVER_RESPONSE);
    }

    private static void buildDetailMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName entityTypeName = meta.getEntityType();
        TypeName idTypeName = meta.getIdType();
        TypeName optionalType = ParameterizedTypeName.get(ClassName.get(Optional.class), entityTypeName);

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idTypeName, ClassName.get(Long.class), "id")
                .addStatement("$T result = repository.findById(id)", optionalType)
                .addStatement("return result.map(value -> $T.ok().body($T.success(value)))" +
                                ".orElse($T.notFound().build())",
                        SERVER_RESPONSE, REST_RESPONSE, SERVER_RESPONSE);
    }

    private static void buildCreateMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName entityTypeName = meta.getEntityType();

        builder.addStatement("$T entity = request.body($T.class)", entityTypeName, entityTypeName)
                .addStatement("$T saved = repository.save(entity)", entityTypeName)
                .addStatement("return $T.ok().body($T.success(saved))",
                        SERVER_RESPONSE, REST_RESPONSE);
    }

    private static void buildUpdateMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName entityTypeName = meta.getEntityType();
        TypeName idTypeName = meta.getIdType();
        TypeName optionalType = ParameterizedTypeName.get(ClassName.get(Optional.class), entityTypeName);

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idTypeName, ClassName.get(Long.class), "id")
                .addStatement("$T entity = request.body($T.class)", entityTypeName, entityTypeName)
                .addStatement("$T result = repository.findById(id)", optionalType)
                .beginControlFlow("if (result.isEmpty())")
                .addStatement("return $T.notFound().build()", SERVER_RESPONSE)
                .endControlFlow()
                .addStatement("repository.save(entity)")
                .addStatement("return $T.ok().body($T.success(entity))",
                        SERVER_RESPONSE, REST_RESPONSE);
    }

    private static void buildDeleteMethod(RepositoryMeta meta, MethodSpec.Builder builder) {
        TypeName idTypeName = meta.getIdType();

        builder.addStatement("$T id = $T.valueOf(request.pathVariable($S))",
                        idTypeName, ClassName.get(Long.class), "id")
                .addStatement("repository.deleteById(id)")
                .addStatement("return $T.ok().body($T.success())",
                        SERVER_RESPONSE, REST_RESPONSE);
    }
}
