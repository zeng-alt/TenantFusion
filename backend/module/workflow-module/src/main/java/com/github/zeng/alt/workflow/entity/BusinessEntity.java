package com.github.zeng.alt.workflow.entity;

import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 业务主表。
 * <p>
 * 以树形结构组织业务节点，每个节点可通过 {@link #formConfigId} 关联一个配置表单
 * （{@link FormConfigEntity}），用于在业务页面渲染对应的动态表单。
 * 采用 parentId 自关联构成业务树。
 *
 * @author zengAlt
 */
@Entity
@Table(name = "wf_business")
@Getter
@Setter
public class BusinessEntity extends BaseEntity<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @SnowflakeId
    @Column(name = "business_id")
    private Long businessId;

    /** 业务名称 */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** 业务编码（全局唯一） */
    @Column(name = "code", nullable = false, unique = true, length = 128)
    private String code;

    /** 父业务ID（根节点为空） */
    @Column(name = "parent_id")
    private Long parentId;

    /** 排序号 */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** 关联配置表单ID */
    @Column(name = "form_config_id")
    private Long formConfigId;

    /** 业务描述 */
    @Column(name = "description", length = 512)
    private String description;

    /** 备注 */
    @Column(name = "remark", length = 512)
    private String remark;

    @Override
    public Long getId() {
        return businessId;
    }
}
