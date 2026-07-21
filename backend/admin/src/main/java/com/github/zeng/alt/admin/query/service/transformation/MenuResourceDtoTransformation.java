package com.github.zeng.alt.admin.query.service.transformation;

import com.github.zeng.alt.admin.infrastructure.entity.MenuResource;
import com.github.zeng.alt.admin.query.api.dto.MenuResourceDto;
import java.util.Collections;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author zengJiaJun
 * @version 1.0
 * @since 2025年04月09日 16:57
 */
@Mapper(componentModel = "spring")
public interface MenuResourceDtoTransformation {


    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", expression = "java(this.to(menuResource.getChildren()))")
    public MenuResourceDto to(MenuResource menuResource);


    default List<MenuResourceDto> to(List<MenuResource> menuResources) {
        if (CollectionUtils.isEmpty(menuResources)) {
            return null;
        }
        menuResources.sort(Comparator.comparingInt(m -> m.getOrder() == null ? 0 : m.getOrder()));
        LinkedList<MenuResourceDto> result = new LinkedList<>();
        for (MenuResource menuResource : menuResources) {
            result.add(this.to(menuResource));
        }
        return result;
    }

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", expression = "java(this.toFilterButton(menuResource.getChildren()))")
    MenuResourceDto toFilterButton(MenuResource menuResource);

    default List<MenuResourceDto> toFilterButton(List<MenuResource> menuResources) {
        if (CollectionUtils.isEmpty(menuResources)) {
            return Collections.emptyList();
        }
        LinkedList<MenuResourceDto> result = new LinkedList<>();
        for (MenuResource menuResource : menuResources) {
//            if ("BUTTON".equals(menuResource.getType())) continue;
            MenuResourceDto menuResourceVO = this.to(menuResource);

            if (!CollectionUtils.isEmpty(menuResource.getChildren())) {
                menuResourceVO.setChildren(this.toFilterButton(menuResource.getChildren()));
            }
            result.add(menuResourceVO);
        }
        return CollectionUtils.isEmpty(result) ? null : result;
    }
}
