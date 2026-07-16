package com.github.zeng.alt.rest.apt.processor;

import com.github.zeng.alt.rest.annotation.CrudRest;
import com.github.zeng.alt.rest.apt.generator.HandlerGenerator;
import com.github.zeng.alt.rest.apt.generator.MapperGenerator;
import com.github.zeng.alt.rest.apt.generator.PatchMapperGenerator;
import com.github.zeng.alt.rest.apt.generator.RouterGenerator;
import com.github.zeng.alt.rest.apt.meta.MethodMeta;
import com.github.zeng.alt.rest.apt.meta.QueryFieldMeta;
import com.github.zeng.alt.rest.apt.meta.RepositoryMeta;
import com.github.zeng.alt.rest.apt.scanner.FieldScanner;
import com.github.zeng.alt.rest.apt.validator.RepositoryValidator;
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
 * CrudRest APT 处理器 — 扫描 {@code @CrudRest} 注解并生成 CRUD REST 接口。
 * <p>职责编排：校验 → 建元 → 字段扫描 → 代码生成。字段扫描委托给 {@link FieldScanner}。</p>
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 2.0
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("com.github.zeng.alt.rest.annotation.CrudRest")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class CrudRestProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Types typeUtils;
    private Elements elementUtils;
    private FieldScanner fieldScanner;
    private Boolean hasSpringDoc;
    private Boolean hasMapStruct;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.typeUtils = processingEnv.getTypeUtils();
        this.elementUtils = processingEnv.getElementUtils();
        this.fieldScanner = new FieldScanner(typeUtils, elementUtils, messager);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(CrudRest.class)) {
            processCrudRest(element);
        }
        return true;
    }

    private void processCrudRest(Element element) {
        if (!RepositoryValidator.validate(element, messager, typeUtils, elementUtils)) {
            return;
        }

        try {
            TypeElement typeElement = (TypeElement) element;
            CrudRest annotation = typeElement.getAnnotation(CrudRest.class);

            RepositoryMeta meta = buildMeta(typeElement, annotation);
            if (meta == null) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "无法解析 BaseRepository 泛型参数: " + typeElement.getSimpleName(), element);
                return;
            }

            boolean useMapStruct = checkMapStruct();

            // 如果项目引入了 MapStruct，先生成 Mapper 接口
            if (useMapStruct) {
                var mapperFile = MapperGenerator.generate(meta, elementUtils);
                mapperFile.writeTo(filer);
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "已生成 MapStruct Mapper: " + meta.getEntitySimpleName() + "Mapper");

                var patchMapperFile = PatchMapperGenerator.generate(meta, elementUtils);
                patchMapperFile.writeTo(filer);
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "已生成 MapStruct Patch Mapper: " + meta.getEntitySimpleName() + "Mapper");
            }

            var handlerFile = HandlerGenerator.generate(meta, useMapStruct, elementUtils);
            handlerFile.writeTo(filer);

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

        String repoPackage = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();

        RepositoryMeta.Builder metaBuilder = RepositoryMeta.builder()
                .repositorySimpleName(typeElement.getSimpleName().toString())
                .repositoryPackageName(repoPackage)
                .generatedPackageName(repoPackage)
                .entityType(entityType)
                .idType(idType)
                .path(annotation.path())
                .pageable(annotation.pageable())
                .sort(annotation.sort())
                .repositoryElement(typeElement)
                .hasSpringDoc(checkSpringDoc());

        // jpa-search-helper 搜索接口
        if (annotation.search()) {
            metaBuilder.addEnabledMethod(MethodMeta.SEARCH);
            metaBuilder.addEnabledMethod(MethodMeta.SEARCH_BODY);
        }

        if (annotation.list()) metaBuilder.addEnabledMethod(MethodMeta.LIST);
        if (annotation.listAll()) metaBuilder.addEnabledMethod(MethodMeta.LIST_ALL);
        if (annotation.detail()) metaBuilder.addEnabledMethod(MethodMeta.DETAIL);
        if (annotation.create()) metaBuilder.addEnabledMethod(MethodMeta.CREATE);
        if (annotation.update()) metaBuilder.addEnabledMethod(MethodMeta.UPDATE);
        if (annotation.patch()) metaBuilder.addEnabledMethod(MethodMeta.PATCH);
        if (annotation.delete()) metaBuilder.addEnabledMethod(MethodMeta.DELETE);

        // 查询字段扫描
        fieldScanner.parseQueryType(metaBuilder, annotation, entityMirror);

        // swapSort + autoSort → 生成批量重排序接口
        if (annotation.sort()) {
            RepositoryMeta tempMeta = metaBuilder.build();
            boolean hasAutoSort = tempMeta.getQueryFields().stream()
                    .anyMatch(QueryFieldMeta::isAutoSort);
            if (hasAutoSort) {
                metaBuilder.addEnabledMethod(MethodMeta.SORT);
            }
        }

        // 操作类型 DTO
        ClassName createType = fieldScanner.parseOperationType(annotation::createType);
        ClassName updateType = fieldScanner.parseOperationType(annotation::updateType);
        ClassName patchType = fieldScanner.parseOperationType(annotation::patchType);
        ClassName detailType = fieldScanner.parseOperationType(annotation::detailType);
        ClassName listType = fieldScanner.parseOperationType(annotation::listType);
        ClassName searchType = fieldScanner.parseOperationType(annotation::searchType);
        if (createType != null) metaBuilder.createType(createType);
        if (updateType != null) metaBuilder.updateType(updateType);
        if (patchType != null) metaBuilder.patchType(patchType);
        if (detailType != null) metaBuilder.detailType(detailType);
        if (listType != null) metaBuilder.listType(listType);
        if (searchType != null) metaBuilder.searchType(searchType);

        // 实体字段扫描（含 @JsonIgnore 的全量字段用于 DTO 转换，过滤后的用于 OpenAPI Schema）
        TypeElement entityElement = (TypeElement) typeUtils.asElement(entityMirror);
        fieldScanner.scanEntityFieldsAll(entityElement, metaBuilder::addEntityAllField);
        fieldScanner.scanEntityFields(entityElement, metaBuilder::addEntityField);

        if (createType != null && !createType.equals(entityType)) {
            TypeElement createTypeEl = elementUtils.getTypeElement(createType.reflectionName());
            if (createTypeEl != null) {
                fieldScanner.scanEntityFields(createTypeEl, metaBuilder::addCreateTypeField);
            }
        }
        if (updateType != null && !updateType.equals(entityType)) {
            TypeElement updateTypeEl = elementUtils.getTypeElement(updateType.reflectionName());
            if (updateTypeEl != null) {
                fieldScanner.scanEntityFields(updateTypeEl, metaBuilder::addUpdateTypeField);
            }
        }
        if (patchType != null && !patchType.equals(entityType)) {
            TypeElement patchTypeEl = elementUtils.getTypeElement(patchType.reflectionName());
            if (patchTypeEl != null) {
                fieldScanner.scanEntityFields(patchTypeEl, metaBuilder::addPatchTypeField);
            }
        }
        if (listType != null && !listType.equals(entityType)) {
            TypeElement listTypeEl = elementUtils.getTypeElement(listType.reflectionName());
            if (listTypeEl != null) {
                fieldScanner.scanEntityFields(listTypeEl, metaBuilder::addListTypeField);
            }
        }
        if (detailType != null && !detailType.equals(entityType)) {
            TypeElement detailTypeEl = elementUtils.getTypeElement(detailType.reflectionName());
            if (detailTypeEl != null) {
                fieldScanner.scanEntityFields(detailTypeEl, metaBuilder::addDetailTypeField);
            }
        }

        if (searchType != null && !searchType.equals(entityType)) {
            TypeElement searchTypeEl = elementUtils.getTypeElement(searchType.reflectionName());
            if (searchTypeEl != null) {
                fieldScanner.scanEntityFields(searchTypeEl, metaBuilder::addSearchTypeField);
            }
        }

        return metaBuilder.build();
    }

    private boolean checkSpringDoc() {
        if (hasSpringDoc == null) {
            hasSpringDoc = elementUtils.getTypeElement(
                    "org.springdoc.core.annotations.RouterOperation") != null;
            if (hasSpringDoc) {
                messager.printMessage(Diagnostic.Kind.NOTE, "检测到 springdoc-openapi，将生成 Swagger 注解");
            }
        }
        return hasSpringDoc;
    }

    /**
     * 检查类路径上是否存在 MapStruct，决定是否生成 Mapper 接口
     */
    private boolean checkMapStruct() {
        if (hasMapStruct == null) {
            hasMapStruct = elementUtils.getTypeElement("org.mapstruct.Mapper") != null;
            if (hasMapStruct) {
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "检测到 MapStruct，将为 CRUD REST 生成 Mapper 接口替换 BeanUtils");
            }
        }
        return hasMapStruct;
    }
}
