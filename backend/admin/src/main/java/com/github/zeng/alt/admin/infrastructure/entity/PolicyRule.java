package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "main_policy_rule")
@Getter @Setter
@SQLDelete(sql = """
    update main_policy_rule
    set is_deleted=true
    where policy_rule_id=?
""")
@SQLRestriction("is_deleted=false")
public class PolicyRule extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long policyRuleId;

    private String name;
    private String description;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(columnDefinition = "text")
    private String condition;

    @Column(name = "is_pre_auth")
    private Boolean preAuth = true;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return policyRuleId;
    }
}