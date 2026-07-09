package com.github.zeng.alt.domain.sort;

import com.github.zeng.alt.rest.annotation.QueryOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PrePersist;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;

/**
 * Entity listener that automatically assigns a sort value to fields annotated
 * with {@link QueryOrder#autoSort()} = {@code true} before persisting an entity.
 * <p>
 * When the annotated field is {@code null}, the listener queries the current
 * maximum value and increments it by 100.
 * <p>
 * Compatible with GraalVM native image. Uses direct field injection
 * ({@link PersistenceContext @PersistenceContext},
 * {@link Autowired @Autowired}) instead of a static
 * {@code ApplicationContext} field, and delegates to
 * {@link ClassUtils#getUserClass(Class)} to handle Hibernate proxies
 * transparently.
 *
 * @author zengJiaJun
 * @version 1.0
 * @since 2025年12月31日
 */
@Component
public class AutoSortEntityListener {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ConversionService conversionService;

    @PrePersist
    public void prePersist(Object entity) {
        try {
            // Resolve the real entity class (handle Hibernate proxies)
            Class<?> entityClass = ClassUtils.getUserClass(entity);

            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
            for (Field field : entityClass.getDeclaredFields()) {

                QueryOrder autoSort = field.getAnnotation(QueryOrder.class);
                if (autoSort == null || !autoSort.autoSort()) {
                    continue;
                }

                String fieldName = field.getName();

                if (wrapper.getPropertyValue(fieldName) != null) {
                    continue;
                }

                String entityName = entityManager.getMetamodel()
                        .entity(entityClass)
                        .getName();

                // Query the current maximum value
                String jpql = String.format(
                        "SELECT MAX(e.%s) FROM %s e",
                        fieldName,
                        entityName
                );

                Number maxValue = entityManager.createQuery(jpql, Number.class)
                        .getSingleResult();

                long newValue = (maxValue == null ? 100 : maxValue.longValue() + 100);

                // Convert to the field's target type via the Spring ConversionService
                Object convertedValue =
                        conversionService.convert(newValue, field.getType());

                wrapper.setPropertyValue(fieldName, convertedValue);
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException("AutoSort processing failed for entity: "
                    + ClassUtils.getUserClass(entity).getName(), e);
        }
    }
}
