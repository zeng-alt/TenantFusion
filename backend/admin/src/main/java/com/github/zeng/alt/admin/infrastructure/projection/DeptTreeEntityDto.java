package com.github.zeng.alt.admin.infrastructure.projection;

import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
public class DeptTreeEntityDto {
    private Long deptId;
    private String deptName;
    private Integer deptSort;
    private Boolean enabled;
    private String remark;
    private List<DeptTreeEntityDto> children;

    public List<DeptTreeEntityDto> getChildren() {
        return CollectionUtils.isEmpty(children)
                ? null
                : children;
    }
}
