package com.github.zeng.alt.api.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author zengJiaJun
 * @since 2026年05月26日
 * @version 1.0
 */
@Data
@Accessors(chain = true)
@Schema(name = "排序信息", description = "排序信息")
public class BaseSortReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "主键")
    private Long id;

    @Schema(name = "排序")
    private Integer sort;
}
