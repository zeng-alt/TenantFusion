package com.github.zeng.alt.admin.interfaces.rest;


import com.github.zeng.alt.admin.query.api.MenuResourceService;
import com.github.zeng.alt.admin.query.api.dto.MenuResourceDto;
import com.github.zeng.alt.api.exception.BaseException;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.security.api.SecurityUser;
import com.github.zeng.alt.security.api.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2025年04月09日 16:35
 */
@Tag(name = "菜单资源管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/menu/resource")
public class MenuResourceController {

    private final MenuResourceService menuResourceService;

    @Operation(summary = "获取当前用户资源树")
    @GetMapping("/tree")
    public RestResponse<List<MenuResourceDto>> tree() {
        SecurityUser securityUser = UserContextHolder.getSecurityUser();
        if (securityUser == null) {
            throw new BaseException("当前用户登录异常");
        }
        String id = securityUser.getId();
        String roleCode = securityUser.getCurrentRole() == null ? null : securityUser.getCurrentRole().getAuthority();
        return RestResponse.success(this.menuResourceService.tree(id, roleCode));
    }

    @Operation(summary = "获取所有菜单资源")
    @GetMapping("/tree/menu")
    public RestResponse<List<MenuResourceDto>> treeMenu() {
        return RestResponse.success(this.menuResourceService.treeMenu());
    }

    /**
     * 获取所有菜单资源及菜单下的按钮
     * @return 菜单资源及菜单下的按钮
     */
    @Operation(summary = "获取所有资源树")
    @GetMapping("/tree/all")
    public RestResponse<List<MenuResourceDto>> treeAll() {
        return RestResponse.success(this.menuResourceService.treeAll());
    }

    /**
     * 获取所有菜单资源及菜单下的按钮
     * @return 菜单资源及菜单下的按钮
     */
    @Operation(summary = "获取所有开启的资源树")
    @GetMapping("/tree/enable/all")
    public RestResponse<List<MenuResourceDto>> treeEnableAll() {
        return RestResponse.success(this.menuResourceService.treeEnableAll());
    }

    @Operation(summary = "获取菜单下的按钮")
    @GetMapping("/button/{id}")
    public RestResponse<List<MenuResourceDto>> button(@PathVariable Long id) {
        return RestResponse.success(this.menuResourceService.button(id));

    }

    @Operation(summary = "验证菜单路径")
    @GetMapping("/validate")
    public RestResponse<Boolean> validateMenuPath(@RequestParam("path") String path) {
        return RestResponse.success(this.menuResourceService.validateMenuPath(path));
    }
}
