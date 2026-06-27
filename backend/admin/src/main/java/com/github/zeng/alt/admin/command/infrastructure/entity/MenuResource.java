package com.github.zeng.alt.admin.command.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.rest.annotation.CrudRest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("MENU")
@Getter @Setter
public class MenuResource extends Permission {

    private String path;
    private String component;
    private String redirect;
    private String icon;
    private String layout;
    private String keepAlive;
    private String menuName;
    private String menuStyle;
    @Column(name = "resource_order")
    private Integer order;

    @Column(name = "is_show")
    private Boolean show = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MenuResource parent;

    @JsonIgnore
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("order ASC")
    private Set<MenuResource> children = new LinkedHashSet<>();
}

