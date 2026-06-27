package com.github.zeng.alt.admin.command.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "main_user_resource")
@Getter @Setter
public class UserResource extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long userResourceId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Permission resource;    // 指向 main_permission.id

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return userResourceId;
    }
}