package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.admin.infrastructure.listener.DictTypeListener;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.domain.validation.UniqueCheck;
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
@EntityListeners(DictTypeListener.class)
@UniqueCheck(field = "dictCode")
public class DictType extends BaseEntity<Long>{

    @Id
    @SnowflakeId
    private Long dictTypeId;

    private String dictName;

    private String dictCode;

    /**
     * 是否默认（Y是 N否）
     */
    private Boolean isDefault = false;

    private String remark;

    @TenantId
    private String tenantBy;

    @Transient
    @JsonIgnore
    private transient Boolean oldIsDefault;


    @Override
    public Long getId(){
        return dictTypeId;
    }
}
