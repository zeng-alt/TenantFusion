package com.github.zeng.alt.admin.command.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "main_policy_rule")
@Getter @Setter
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

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return policyRuleId;
    }
}