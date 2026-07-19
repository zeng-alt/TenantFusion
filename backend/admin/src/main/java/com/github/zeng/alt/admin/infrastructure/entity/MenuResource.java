package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.api.base.BaseTreeEntity;
import com.github.zeng.alt.rest.annotation.QueryOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.util.*;

@Entity
@DiscriminatorValue("MENU")
@Getter @Setter
public class MenuResource extends Permission implements BaseTreeEntity<MenuResource> {

    @QueryOrder
    private String path;
    private String component;
    private String redirect;
    private String icon;
    private String layout;
    private String keepAlive;
    private String menuName;
    private String menuStyle;

    @QueryOrder(autoSort = true)
    @Column(name = "resource_order")
    private Integer order;

    @Column(name = "is_show")
    private Boolean show = true;

    @Transient
    private Long parentId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MenuResource parent;

    public void addChild(MenuResource child){
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(MenuResource child){
        children.remove(child);
        child.setParent(null);
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
        if (parentId != null) {
            this.parent = new MenuResource();
            this.parent.setPermissionId(parentId);
        } else {
            this.parent = null;
        }
    }

    @JsonIgnore
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("order ASC")
    private List<MenuResource> children = new LinkedList<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        MenuResource that = (MenuResource) o;
        return getPermissionId() != null && Objects.equals(getPermissionId(), that.getPermissionId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}

