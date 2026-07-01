package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("HTTP")
@Getter @Setter
public class HttpResource extends Permission {

    @Column(length = 10)
    private String method;              // GET, POST, PUT, DELETE
    private String path;                // /api/users
    private String redirect;
    private String buttonName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private MenuResource menu;          // 所属菜单，可为空
}