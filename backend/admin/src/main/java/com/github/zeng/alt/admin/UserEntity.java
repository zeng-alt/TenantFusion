package com.github.zeng.alt.admin;

import com.github.zeng.alt.domain.base.BaseEntity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author zengJiaJun
 * @since 2026年05月28日
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper=false)
public class UserEntity extends BaseEntity<Long> {

    @Id
    @GeneratedValue
    private Long id;

    private String username;
}
