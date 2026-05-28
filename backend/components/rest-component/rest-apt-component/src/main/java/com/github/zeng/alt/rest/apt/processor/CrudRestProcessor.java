package com.github.zeng.alt.rest.apt.processor;


import com.github.zeng.alt.rest.apt.generator.HandlerGenerator;
import com.github.zeng.alt.rest.apt.generator.RouterGenerator;
import com.github.zeng.alt.rest.apt.meta.MethodMeta;
import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.github.zeng.alt.rest.apt.validator.RepositoryValidator;
import com.github.zeng.alt.rest.annotation.CrudRest;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * CrudRest APT 处理器 — 扫描 @CrudRest 注解并生成 CRUD REST 接口
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 1.0
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.github.zeng.alt.rest.annotation.CrudRest")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CrudRestProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.typeUtils = processingEnv.getTypeUtils();
        this.elementUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(CrudRest.class)) {
            processCrudRest(element);
        }
        return true;
    }


    private void processCrudRest(Element element) {
        // 校验
        if (!RepositoryValidator.validate(element, messager, typeUtils, elementUtils)) {
            return;
        }

        try {
            TypeElement typeElement = (TypeElement) element;
            CrudRest annotation = typeElement.getAnnotation(CrudRest.class);

            // 构建 RepositoryMeta
            RepositoryMeta meta = buildMeta(typeElement, annotation);
            if (meta == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "无法解析 BaseRepository 泛型参数: " + typeElement.getSimpleName(), element);
                return;
            }

            // 生成 Handler
            var handlerFile = HandlerGenerator.generate(meta);
            handlerFile.writeTo(filer);

            // 生成 Router
            var routerFile = RouterGenerator.generate(meta);
            routerFile.writeTo(filer);

            messager.printMessage(Diagnostic.Kind.NOTE,
                    "已生成 CRUD REST: " + meta.getPath()
                            + " -> " + meta.getGeneratedPackageName()
                            + "." + meta.getHandlerSimpleName()
                            + ", " + meta.getRouterSimpleName());
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "生成代码时发生 I/O 错误: " + e.getMessage(), element);
        }
    }

    private RepositoryMeta buildMeta(TypeElement typeElement, CrudRest annotation) {
        List<? extends TypeMirror> typeArgs =
                RepositoryValidator.getBaseRepositoryTypeArgs(typeElement, typeUtils, elementUtils);
        if (typeArgs == null || typeArgs.size() < 2) {
            return null;
        }

        TypeMirror entityMirror = typeArgs.get(0);
        TypeMirror idMirror = typeArgs.get(1);

        ClassName entityType = ClassName.get((TypeElement) typeUtils.asElement(entityMirror));
        ClassName idType = ClassName.get((TypeElement) typeUtils.asElement(idMirror));

        String repoPackage = processingEnv.getElementUtils()
                .getPackageOf(typeElement).getQualifiedName().toString();

        // 生成包名 = Repository 所在包 + ".rest"
        String generatedPackage = repoPackage + ".rest";

        // 构建启用的方法列表
        RepositoryMeta.Builder metaBuilder = RepositoryMeta.builder()
                .repositorySimpleName(typeElement.getSimpleName().toString())
                .repositoryPackageName(repoPackage)
                .generatedPackageName(generatedPackage)
                .entityType(entityType)
                .idType(idType)
                .path(annotation.path())
                .pageable(annotation.pageable())
                .repositoryElement(typeElement);

        if (annotation.list()) metaBuilder.addEnabledMethod(MethodMeta.LIST);
        if (annotation.detail()) metaBuilder.addEnabledMethod(MethodMeta.DETAIL);
        if (annotation.create()) metaBuilder.addEnabledMethod(MethodMeta.CREATE);
        if (annotation.update()) metaBuilder.addEnabledMethod(MethodMeta.UPDATE);
        if (annotation.delete()) metaBuilder.addEnabledMethod(MethodMeta.DELETE);

        return metaBuilder.build();
    }
}
