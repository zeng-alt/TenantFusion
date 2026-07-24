package com.github.zeng.alt.domain.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import java.lang.reflect.Method;

/**
 * {@link UniqueCheck} 的校验器。
 * <p>
 * 注解在类上，{@code isValid(Object value, ...)} 的 {@code value} 即为被校验的对象本身（rootBean），
 * 通过 getter 方法获取字段值和主键值，无需依赖 Hibernate Validator 内部 API。
 *
 * @author zengJiaJun
 * @since 2026年07月23日
 */
public class UniqueValidator implements ConstraintValidator<UniqueCheck, Object> {

    private UniqueCheck annotation;

    @Override
    public void initialize(UniqueCheck constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        IUniqueCheckRepository repository = UniqueCheckServiceHolder.getRepository();
        if (repository == null) {
            return true;
        }

        String fieldName = annotation.field();

        Class<?> entityClass = annotation.entity();
        if (entityClass == void.class) {
            entityClass = value.getClass();
        }

        try {
            Object fieldValue = invokeGetter(value, fieldName);
            Object idValue = invokeGetter(value, annotation.idField());

            boolean unique = repository.isUnique(entityClass, fieldName, fieldValue, idValue, annotation.ignoreCase());
            if (!unique) {
                context.disableDefaultConstraintViolation();

                HibernateConstraintValidatorContext hibernateContext =
                        context.unwrap(HibernateConstraintValidatorContext.class);

                hibernateContext
                        .addMessageParameter("field", fieldName)
                        .buildConstraintViolationWithTemplate(annotation.message())
                        .addPropertyNode(fieldName)
                        .addConstraintViolation();

                return false;
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to check uniqueness for: " + fieldName, e);
        }
    }

    private static Object invokeGetter(Object object, String fieldName) throws Exception {
        if (fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (Class<?> clazz = object.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Method getter = clazz.getDeclaredMethod(getterName);
                return getter.invoke(object);
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(
                "No getter found for field '" + fieldName + "' in " + object.getClass().getName());
    }
}
