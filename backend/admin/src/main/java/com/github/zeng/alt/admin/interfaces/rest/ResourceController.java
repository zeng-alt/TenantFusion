package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.admin.infrastructure.entity.HttpResource;
import com.github.zeng.alt.admin.infrastructure.repository.HttpResourceRepository;
import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "资源管理")
@RestController
@RequestMapping("/v1/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final HttpResourceRepository httpResourceRepository;

    @Operation(summary = "分页查询HTTP资源（支持按menuId筛选）")
    @GetMapping("/http/page")
    public PageRestResponse<HttpResource> pageHttp(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long menuId) {
        var pageable = org.springframework.data.domain.PageRequest.of(page - 1, size);
        var pageResult = menuId != null
            ? httpResourceRepository.findByMenu_Id(menuId, pageable)
            : httpResourceRepository.findAll(pageable);
        return PageRestResponse.of(pageResult.getContent(), pageResult.getTotalElements(), size, page);
    }

    @Operation(summary = "批量关联HTTP资源到菜单")
    @PostMapping("/http/associate")
    @Transactional
    public RestResponse<Void> associateHttp(@RequestBody List<Map<String, Long>> resources) {
        for (var item : resources) {
            var opt = httpResourceRepository.findById(item.get("id"));
            if (opt.isDefined()) {
                var resource = opt.get();
                resource.setMenuId(item.get("menuId"));
                httpResourceRepository.save(resource);
            }
        }
        return RestResponse.success();
    }

    @Operation(summary = "取消HTTP资源的菜单关联")
    @PatchMapping("/http/{id}/disconnect")
    @Transactional
    public RestResponse<Void> disconnectHttp(@PathVariable Long id) {
        var opt = httpResourceRepository.findById(id);
        if (opt.isDefined()) {
            var resource = opt.get();
            resource.setMenuId(null);
            httpResourceRepository.save(resource);
        }
        return RestResponse.success();
    }
}
