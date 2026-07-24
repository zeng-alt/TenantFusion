package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.admin.infrastructure.listener.DictDataListener;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.domain.validation.UniqueCheck;
import com.github.zeng.alt.rest.annotation.QueryField;
import com.github.zeng.alt.rest.annotation.QueryOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import org.springframework.lang.Nullable;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2025年05月16日 16:52
 */
@Getter
@Setter
@Entity
@Table(name = "main_dict_data")
@EntityListeners(DictDataListener.class)
@UniqueCheck(field = "dictValue")
public class DictData extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long dictDataId;

    /**
     * 字典排序
     */
    @QueryOrder(autoSort = true)
    private Integer dictSort;

    /**
     * 字典标签
     */
    private String dictLabel;

    /**
     * 字典键值
     */
    private String dictValue;

    /**
     * 样式属性（其他样式扩展）
     */
    private String cssClass;

    /**
     * 表格字典样式
     */
    private String listClass;

    /**
     * 是否默认（Y是 N否）
     */
    private Boolean isDefault = false;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态（0正常 1停用）
     */
    @Column(name = "is_enabled")
    private Boolean enabled = true;

    @TenantId
    @Nullable
    private String tenantBy;

    /**
     * 字典类型编码
     */
    @QueryField
    private String dictCode;


    @Transient
    @JsonIgnore
    private transient Boolean oldIsDefault;

    @Override
    public @org.jspecify.annotations.Nullable Long getId() {
        return dictDataId;
    }
}
