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
@Table(name = "main_user_role")
@Getter @Setter
@SQLDelete(sql = """
    update main_user_role
    set is_deleted=true
    where user_role_id=?
""")
@SQLRestriction("is_deleted=false")
public class UserRole extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long userRoleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return userRoleId;
    }
}