package com.github.zeng.alt.admin.query.service;

import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.admin.infrastructure.entity.MenuResource;
import com.github.zeng.alt.admin.infrastructure.entity.Permission;
import com.github.zeng.alt.admin.infrastructure.entity.QMenuResource;
import com.github.zeng.alt.admin.infrastructure.repository.MenuResourceRepository;
import com.github.zeng.alt.admin.infrastructure.repository.PermissionRepository;
import com.github.zeng.alt.admin.query.api.MenuResourceService;
import com.github.zeng.alt.admin.query.api.dto.MenuResourceDto;
import com.github.zeng.alt.admin.query.service.transformation.MenuResourceDtoTransformation;
import com.github.zeng.alt.security.core.properties.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2025年04月09日 16:43
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MenuResourceServiceImpl implements MenuResourceService {

    private final MenuResourceRepository menuResourceDao;
    private final PermissionRepository permissionDao;
    private final MenuResourceDtoTransformation transformation;
    private final SecurityProperties securityProperties;

    @Transactional(readOnly = true)
    public List<MenuResourceDto> tree(String id, String roleCode) {

        List<Permission> permissions =
                securityProperties.getAdmin().getId().equals(id) || securityProperties.getAdmin().getCode().equalsIgnoreCase(roleCode)
                        ? loadAllPermissions()
                        : loadPermissionsByRoleCode(roleCode);

        return buildPermissionTree(permissions);
    }



    private List<MenuResourceDto> buildMenuTree(List<MenuResource> menus) {

        Map<Long, MenuResourceDto> dtoMap = new HashMap<>();
        List<MenuResourceDto> roots = new ArrayList<>();

        // 1. 转 DTO
        for (MenuResource menu : menus) {
            MenuResourceDto dto = toDto(menu);
            dto.setChildren(new ArrayList<>());
            dtoMap.put(menu.getId(), dto);
        }

        // 2. 建树
        for (MenuResource menu : menus) {
            MenuResourceDto current = dtoMap.get(menu.getId());

            if (menu.getParent() == null) {
                roots.add(current);
            } else {
                MenuResourceDto parent = dtoMap.get(menu.getParent().getId());
                if (parent != null) {
                    parent.getChildren().add(current);
                }
            }
        }

        // 3. 排序
        sortTree(roots);

        return roots;
    }

    private void attachApiResources(
            List<MenuResourceDto> menus,
            Map<Long, List<HttpResource>> httpMap
    ) {
        if (menus == null || menus.isEmpty()) {
            return;
        }

        for (MenuResourceDto menu : menus) {

            Long menuId = menu.getId();

            // 确保 children 不为 null
            if (menu.getChildren() == null) {
                menu.setChildren(new ArrayList<>());
            }

            // 1️⃣ 挂 HTTP
            List<HttpResource> httpList = httpMap.get(menuId);
            if (httpList != null) {
                for (HttpResource http : httpList) {
                    menu.getChildren().add(toDto(http));
                }
            }
            
            // 3️⃣ 递归处理子菜单
            attachApiResources(menu.getChildren(), httpMap);

            // 4️⃣ 排序
            sortTree(menu.getChildren());
        }
    }


    private void sortTree(List<MenuResourceDto> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        list.sort(
                Comparator.comparing(
                        MenuResourceDto::getOrder,
                        Comparator.nullsLast(Integer::compareTo)
                )
        );

        for (MenuResourceDto dto : list) {
            if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
                sortTree(dto.getChildren());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuResourceDto> treeMenu() {
        List<MenuResource> menus = menuResourceDao.findAll();
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        return buildMenuTree(menus);
    }


    @Override
    public List<MenuResourceDto> treeAll() {

        List<Permission> permissions = loadAllPermissions();
        return buildPermissionTree(permissions);
    }

    @Override
    public List<MenuResourceDto> treeEnableAll() {
        List<Permission> permissions = permissionDao.findAllEnablePermissions();
        return buildPermissionTree(permissions);
    }


    @Override
    public List<MenuResourceDto> button(Long id) {
        QMenuResource qMenuResource = QMenuResource.menuResource;
        List<MenuResourceDto> result = new LinkedList<>();
        for (MenuResource menuResource : menuResourceDao.findAll(qMenuResource.parent.permissionId.eq(id))) {
            result.add(this.transformation.to(menuResource));
        }
        return result;
    }

    @Override
    public Boolean validateMenuPath(String path) {
        return menuResourceDao.existsByPath(path);
    }

    private MenuResourceDto toDto(MenuResource menu) {
        MenuResourceDto dto = new MenuResourceDto();
        dto.setId(menu.getId());
        dto.setParentId(menu.getParent() == null ? null : menu.getParent().getId());
        dto.setName(menu.getName());
        dto.setCode(menu.getCode());
        dto.setType(menu.getResourceType());
        dto.setPath(menu.getPath());
        dto.setIcon(menu.getIcon());
        dto.setComponent(menu.getComponent());
        dto.setLayout(menu.getLayout());
        dto.setKeepAlive(menu.getKeepAlive());
        dto.setShow(menu.getShow());
        dto.setEnabled(menu.getEnabled());
        dto.setMenuStyle(menu.getMenuStyle());
        dto.setOrder(menu.getOrder());
        return dto;
    }

    private MenuResourceDto toDto(HttpResource http) {
        MenuResourceDto dto = new MenuResourceDto();

        dto.setId(http.getId());
        dto.setParentId(http.getId());

        dto.setName(http.getName());
        dto.setCode(http.getCode());

        // 类型标识（前端/权限判断用）
        dto.setType("BUTTON");

        dto.setPath(http.getPath());
        dto.setRedirect(http.getRedirect());
        dto.setMethod(http.getMethod());

        dto.setEnabled(http.getEnabled());
        dto.setShow(false); // HTTP 资源一般不展示为菜单


        dto.setChildren(List.of()); // 叶子节点

        return dto;
    }

    private List<Permission> loadPermissionsByRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return List.of();
        }
        return permissionDao.findPermissionsByRoleCode(roleCode);
    }

    private List<Permission> loadAllPermissions() {
        return permissionDao.findAllPermissions();
    }

    private List<MenuResourceDto> buildPermissionTree(List<Permission> permissions) {

        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }

        List<MenuResource> menus = new ArrayList<>();
        Map<Long, List<HttpResource>> httpMap = new HashMap<>();

        for (Permission p : permissions) {
            if (p instanceof MenuResource menu) {
                menus.add(menu);
            } else if (p instanceof HttpResource http) {
                httpMap
                        .computeIfAbsent(
                                http.getMenuId() == null ? 0L : http.getMenuId(),
                                k -> new ArrayList<>()
                        )
                        .add(http);
            }
        }

        List<MenuResourceDto> rootMenus = buildMenuTree(menus);
        attachApiResources(rootMenus, httpMap);

        return rootMenus;
    }

}
