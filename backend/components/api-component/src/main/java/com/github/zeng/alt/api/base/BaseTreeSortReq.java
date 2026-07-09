package com.github.zeng.alt.api.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author zengJiaJun
 * @since 2026年05月26日
 * @version 1.0
 */
@Data
@Accessors(chain = true)
@Schema(name = "树排序信息", description = "排序信息")
public class BaseTreeSortReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "主键")
    private Long id;

    @Schema(name = "子节点")
    private List<BaseTreeSortReq> children = Collections.emptyList();
}
