package com.github.zeng.alt.workflow.mapper;

import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Optional → 值 的通用转换，供 MapStruct 通过 {@code uses} 注入调用。
 * <p>
 * BaseEntity 的 createdBy / createdDate / lastModifiedDate 以 Optional 暴露，
 * VO 中为直接值，MapStruct 需借助本方法解包。
 *
 * @author zengAlt
 */
@Component
public class OptionalMapper {

    /**
     * 解包 Optional，空值返回 null
     *
     * @param value 可选值
     * @return 解包后的值
     */
    public <T> T map(Optional<T> value) {
        return value == null ? null : value.orElse(null);
    }
}