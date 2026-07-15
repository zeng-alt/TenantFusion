package com.github.zeng.alt.admin.query.api;


import com.github.zeng.alt.admin.query.api.dto.MenuResourceDto;

import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @crateTime 2025年04月09日 16:43
 */
public interface MenuResourceService {

    public List<MenuResourceDto> tree(String username, String roleCode);

    public List<MenuResourceDto> treeMenu();

    public List<MenuResourceDto> treeAll();

    public List<MenuResourceDto> treeEnableAll();

    List<MenuResourceDto> button(Long id);

    Boolean validateMenuPath(String path);
}
