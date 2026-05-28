package com.github.zeng.alt.rest.apt.validator;

import com.github.zeng.alt.domain.base.BaseRepository;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;

/**
 * Repository 校验器 — 校验被 @CrudRest 标注的类型
 *
 * @author zengJiaJun
 * @crateTime 2026年05月28日
 * @version 1.0
 */
public final class RepositoryValidator {

    private RepositoryValidator() {
    }

    /**
     * 校验被 @CrudRest 标注的 TypeElement
     *
     * @param element  被标注的元素
     * @param messager 消息输出
     * @param typeUtils 类型工具
     * @return true 如果校验通过
     */
    public static boolean validate(Element element, Messager messager, Types typeUtils) {
        if (element.getKind() != ElementKind.INTERFACE) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@CrudRest 只能标注在接口上: " + element.getSimpleName(), element);
            return false;
        }

        TypeElement typeElement = (TypeElement) element;

        // 检查是否继承 BaseRepository
        if (!extendsBaseRepository(typeElement, typeUtils)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@CrudRest 标注的接口必须继承 BaseRepository<T, ID>: "
                            + element.getSimpleName(), element);
            return false;
        }

        // 检查泛型参数
        List<? extends TypeMirror> typeArgs = getBaseRepositoryTypeArgs(typeElement, typeUtils);
        if (typeArgs == null || typeArgs.size() < 2) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "BaseRepository 必须指定两个泛型参数 <T, ID>: "
                            + element.getSimpleName(), element);
            return false;
        }

        return true;
    }

    /**
     * 判断 TypeElement 是否继承 BaseRepository
     */
    private static boolean extendsBaseRepository(TypeElement typeElement, Types typeUtils) {
        TypeMirror baseRepoMirror = typeUtils.erasure(
                typeUtils.getDeclaredType(
                        typeUtils.elementForClass(BaseRepository.class)
                )
        );

        // 检查所有父接口
        for (TypeMirror iface : typeElement.getInterfaces()) {
            TypeMirror erased = typeUtils.erasure(iface);
            if (typeUtils.isSameType(erased, baseRepoMirror)) {
                return true;
            }
            // 递归检查父接口的父接口
            Element ifaceElement = typeUtils.asElement(iface);
            if (ifaceElement instanceof TypeElement) {
                if (extendsBaseRepository((TypeElement) ifaceElement, typeUtils)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 获取 BaseRepository 的泛型参数
     */
    public static List<? extends TypeMirror> getBaseRepositoryTypeArgs(TypeElement typeElement, Types typeUtils) {
        TypeMirror baseRepoMirror = typeUtils.erasure(
                typeUtils.getDeclaredType(
                        typeUtils.elementForClass(BaseRepository.class)
                )
        );

        for (TypeMirror iface : typeElement.getInterfaces()) {
            TypeMirror erased = typeUtils.erasure(iface);
            if (typeUtils.isSameType(erased, baseRepoMirror)) {
                if (iface instanceof DeclaredType) {
                    return ((DeclaredType) iface).getTypeArguments();
                }
            }
        }

        // 递归查找
        for (TypeMirror iface : typeElement.getInterfaces()) {
            Element ifaceElement = typeUtils.asElement(iface);
            if (ifaceElement instanceof TypeElement) {
                List<? extends TypeMirror> args = getBaseRepositoryTypeArgs(
                        (TypeElement) ifaceElement, typeUtils);
                if (args != null) {
                    return args;
                }
            }
        }

        return null;
    }
}
