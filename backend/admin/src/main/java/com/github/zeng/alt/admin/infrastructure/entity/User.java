package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.zeng.alt.domain.base.BaseEntity;
import com.github.zeng.alt.domain.key.SnowflakeId;
import com.github.zeng.alt.rest.annotation.QueryField;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "main_user")
@Getter
@Setter
@SQLDelete(sql = """
    update main_user
    set is_deleted=true
    where user_id=?
""")
@SQLRestriction("is_deleted=false")
public class User extends BaseEntity<Long> {

    @Id @SnowflakeId
    private Long userId;

    @Column(length = 64)
    @QueryField
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String nickName;
    private String avatar;
    private String email;
    private String phoneNumber;
    @QueryField
    private String gender;

    @QueryField
    @Column(name = "is_enabled")
    private Boolean enabled = true;

    @Column(name = "is_deleted")
    private Boolean deleted = false;

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<UserRole> userRoles = new LinkedList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<UserResource> userResources = new LinkedList<>();

    @Override
    @JsonIgnore
    public @Nullable Long getId() {
        return userId;
    }
}