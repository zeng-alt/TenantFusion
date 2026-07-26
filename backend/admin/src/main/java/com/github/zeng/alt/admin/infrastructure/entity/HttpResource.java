package com.github.zeng.alt.admin.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zeng.alt.admin.infrastructure.listener.HttpResourceListener;
import com.github.zeng.alt.rest.annotation.QueryField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("HTTP")
@Getter @Setter
@Schema(title = "http资源")
@EntityListeners(HttpResourceListener.class)
public class HttpResource extends Permission {

    @Column(length = 10)
    @QueryField
    @Schema(name = "请求方法")
    private String method;              // GET, POST, PUT, DELETE
    @Schema(name = "请求路径")
    private String path;                // /api/users
    private String redirect;
    private String buttonName;

    @Transient
    @JsonIgnore
    private transient String oldPath;

    @Transient
    @JsonIgnore
    private transient String oldMethod;

    /**
     * 数据库字段 menu_id
     * 用于查询
     */
    @Column(name = "menu_id")
    @QueryField
    private Long menuId;

    @Transient
    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        if (menuId != null) {
            this.menuId = menuId;
        } else {
            this.menuId = null;
        }
    }
}