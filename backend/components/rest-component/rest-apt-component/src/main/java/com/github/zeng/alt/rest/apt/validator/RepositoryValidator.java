package com.github.zeng.alt.rest.apt.validator;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.List;

public final class RepositoryValidator {

    private static final String BASE_REPOSITORY_CLASS_NAME = "com.github.zeng.alt.domain.base.BaseRepository";

    private RepositoryValidator() {
    }

    public static boolean validate(Element element, Messager messager, Types typeUtils, Elements elementUtils) {
        if (element.getKind() != ElementKind.INTERFACE) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@CrudRest 只能标注在接口上: " + element.getSimpleName(), element);
            return false;
        }

        TypeElement typeElement = (TypeElement) element;

        if (!extendsBaseRepository(typeElement, typeUtils, elementUtils)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "@CrudRest 标注的接口必须继承 BaseRepository<T, ID>: "
                            + element.getSimpleName(), element);
            return false;
        }

        List<? extends TypeMirror> typeArgs = getBaseRepositoryTypeArgs(typeElement, typeUtils, elementUtils);
        if (typeArgs == null || typeArgs.size() < 2) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "BaseRepository <T, ID> 泛型参数缺失: "
                            + element.getSimpleName(), element);
            return false;
        }

        return true;
    }

    private static TypeMirror getBaseRepositoryErasure(Types typeUtils, Elements elementUtils) {
        TypeElement baseRepoElement = elementUtils.getTypeElement(BASE_REPOSITORY_CLASS_NAME);
        if (baseRepoElement == null) {
            throw new IllegalStateException("BaseRepository 类型未找到: " + BASE_REPOSITORY_CLASS_NAME);
        }
        return typeUtils.erasure(typeUtils.getDeclaredType(baseRepoElement));
    }

    private static boolean extendsBaseRepository(TypeElement typeElement, Types typeUtils, Elements elementUtils) {
        TypeMirror baseRepoErasure = getBaseRepositoryErasure(typeUtils, elementUtils);

        for (TypeMirror iface : typeElement.getInterfaces()) {
            TypeMirror erased = typeUtils.erasure(iface);
            if (typeUtils.isSameType(erased, baseRepoErasure)) {
                return true;
            }
            Element ifaceElement = typeUtils.asElement(iface);
            if (ifaceElement instanceof TypeElement) {
                if (extendsBaseRepository((TypeElement) ifaceElement, typeUtils, elementUtils)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static List<? extends TypeMirror> getBaseRepositoryTypeArgs(
            TypeElement typeElement, Types typeUtils, Elements elementUtils) {
        TypeMirror baseRepoErasure = getBaseRepositoryErasure(typeUtils, elementUtils);

        for (TypeMirror iface : typeElement.getInterfaces()) {
            TypeMirror erased = typeUtils.erasure(iface);
            if (typeUtils.isSameType(erased, baseRepoErasure)) {
                if (iface instanceof DeclaredType) {
                    return ((DeclaredType) iface).getTypeArguments();
                }
            }
        }

        for (TypeMirror iface : typeElement.getInterfaces()) {
            Element ifaceElement = typeUtils.asElement(iface);
            if (ifaceElement instanceof TypeElement) {
                List<? extends TypeMirror> args = getBaseRepositoryTypeArgs(
                        (TypeElement) ifaceElement, typeUtils, elementUtils);
                if (args != null) {
                    return args;
                }
            }
        }

        return null;
    }
}
