package com.github.zeng.alt.rest.apt.generator;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.*;

/**
 * 生成器工具方法。
 *
 * @author zengJiaJun
 * @since 2026年07月07日
 * @version 1.0
 */
public final class GeneratorUtils {

    private static final Set<String> SCALAR_TYPES = Set.of(
            "String", "Integer", "Long", "Boolean", "Double", "Float",
            "BigDecimal", "BigInteger",
            "LocalDate", "LocalDateTime", "LocalTime", "Date"
    );

    private GeneratorUtils() {}

    /**
     * 判断类型是否为标量类型（基本类型包装类、常用日期类型）。
     * <p>MapStruct / BeanUtils 可自动处理此类类型，无需生成嵌套转换方法。</p>
     */
    public static boolean isScalarSimpleName(String name) {
        return SCALAR_TYPES.contains(name);
    }

    /**
     * 从全限定名中提取 raw type（去掉泛型参数）。
     * <pre>{@code
     *   "java.util.List<com.example.AddressDto>" → "java.util.List"
     *   "com.example.AddressDto" → "com.example.AddressDto"
     * }</pre>
     */
    public static String extractRawType(String qualifiedName) {
        if (qualifiedName == null) return null;
        int genericStart = qualifiedName.indexOf('<');
        return genericStart >= 0 ? qualifiedName.substring(0, genericStart).trim() : qualifiedName.trim();
    }

    /**
     * 从全限定名中提取 simple name。
     * <pre>{@code
     *   "com.example.AddressDto" → "AddressDto"
     *   "java.util.List<com.example.AddressDto>" → "List"
     * }</pre>
     */
    public static String simpleName(String qualifiedName) {
        if (qualifiedName == null) return "";
        String raw = extractRawType(qualifiedName);
        int dot = raw.lastIndexOf('.');
        return dot >= 0 ? raw.substring(dot + 1) : raw;
    }

    /**
     * 解析全限定名中的泛型类型参数。
     * <pre>{@code
     *   "java.util.List<com.example.AddressDto>" → ["com.example.AddressDto"]
     *   "java.util.Map<String, com.example.AddressDto>" → ["String", "com.example.AddressDto"]
     * }</pre>
     */
    public static List<String> extractTypeParameters(String qualifiedName) {
        if (qualifiedName == null) return List.of();
        int genericStart = qualifiedName.indexOf('<');
        if (genericStart < 0) return List.of();
        int genericEnd = qualifiedName.lastIndexOf('>');
        if (genericEnd <= genericStart) return List.of();
        String inner = qualifiedName.substring(genericStart + 1, genericEnd);
        return parseTypeArguments(inner);
    }

    private static List<String> parseTypeArguments(String inner) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                result.add(inner.substring(start, i).trim());
                start = i + 1;
            }
        }
        String last = inner.substring(start).trim();
        if (!last.isEmpty()) {
            result.add(last);
        }
        return result;
    }

    /**
     * 判断全限定名是否为 Java / javax 标准库类型。
     */
    public static boolean isJavaStandardLibrary(String qualifiedName) {
        if (qualifiedName == null) return true;
        String raw = extractRawType(qualifiedName);
        return raw.startsWith("java.") || raw.startsWith("javax.")
                || raw.startsWith("jakarta.") || raw.startsWith("org.springframework.");
    }

    /**
     * 递归收集一个类型的所有非静态字段（含父类继承的字段）。
     * <p>返回字段名 → 字段类型全限定名的映射。</p>
     */
    public static Map<String, String> collectFields(TypeElement typeElement, Elements elements) {
        Map<String, String> fields = new LinkedHashMap<>();
        collectFieldsRecursive(typeElement, elements, fields);
        return fields;
    }

    private static void collectFieldsRecursive(TypeElement typeElement, Elements elements,
                                                Map<String, String> fields) {
        // 先收集父类字段
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass != null) {
            String superQName = superclass.toString();
            if (!"java.lang.Object".equals(superQName) && !isJavaStandardLibrary(superQName)) {
                TypeElement superEl = elements.getTypeElement(extractRawType(superQName));
                if (superEl != null) {
                    collectFieldsRecursive(superEl, elements, fields);
                }
            }
        }

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.FIELD) continue;
            if (enclosed.getModifiers().contains(Modifier.STATIC)) continue;
            String fieldName = enclosed.getSimpleName().toString();
            if (!fields.containsKey(fieldName)) {  // 子类覆盖父类，优先子类
                fields.put(fieldName, enclosed.asType().toString());
            }
        }
    }

    /**
     * 判断一个类型是否不需要递归扫描其字段。
     * <p>包括：标准库类型、标量类型、无法通过 {@link Elements#getTypeElement} 查找的类型。</p>
     */
    public static boolean isNonIntrospectable(String qualifiedName, Elements elements) {
        if (qualifiedName == null) return true;
        String raw = extractRawType(qualifiedName);
        // 标量类型无需递归
        if (isScalarSimpleName(simpleName(raw))) return true;
        // 标准库类型跳过
        if (isJavaStandardLibrary(raw)) return true;
        // 无法解析的类型跳过
        TypeElement el = elements.getTypeElement(raw);
        return el == null;
    }
}
