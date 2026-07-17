package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.rest.annotation.QueryField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("HTTP")
@Getter @Setter
//@Comment("http资源")
@Schema(title = "http资源")
public class HttpResource extends Permission {

    @Column(length = 10)
//    @Comment("请求方法")
    @QueryField
    @Schema(name = "请求方法")
    private String method;              // GET, POST, PUT, DELETE
//    @Comment("请求路径")
    @Schema(name = "请求路径")
    private String path;                // /api/users
    @QueryField
    private String redirect;
    private String buttonName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id")
    private MenuResource menu;          // 所属菜单，可为空

    @Transient
    public Long getMenuId() {
        return menu != null ? menu.getId() : null;
    }

    public void setMenuId(Long menuId) {
        if (menuId != null) {
            this.menu = new MenuResource();
            this.menu.setPermissionId(menuId);
        } else {
            this.menu = null;
        }
    }
}