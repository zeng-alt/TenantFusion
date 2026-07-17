package com.github.zeng.alt.admin.infrastructure.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年05月16日 16:50
 */
@Entity
@Table(name="main_dict_type")
@Getter
@Setter
public class DictType extends BaseEntity<Long>{

    @Id
    @SnowflakeId
    private Long dictTypeId;

    private String dictName;

    private String dictCode;

    private String remark;

    @TenantId
    private String tenantBy;


    @Override
    public Long getId(){
        return dictTypeId;
    }
}
