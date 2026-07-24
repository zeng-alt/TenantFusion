package com.github.zeng.alt.domain.validation;

/**
 * 唯一性校验查询 SPI。用户需实现此接口，并在 Spring 中注册为 Bean。
 * <p>
 * 当 {@code @UniqueCheck} 校验时，会通过此接口查询数据库中是否存在重复记录。
 *
 * @see UniqueCheck
 */
public interface IUniqueCheckRepository {

    /**
     * 检查指定字段值在数据库中是否唯一。
     *
     * @param entityClass 实体类
     * @param field       字段名
     * @param value       字段值
     * @param id          当前记录 ID（更新时排除自身），可为 {@code null}
     * @return {@code true} 表示唯一，{@code false} 表示已存在
     */
    boolean isUnique(Class<?> entityClass, String field, Object value, Object id, boolean ignoreCase);
}
