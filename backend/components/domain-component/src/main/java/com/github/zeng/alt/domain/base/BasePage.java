package com.github.zeng.alt.domain.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author zengJiaJun
 * @crateTime 2025年12月15日 13:59
 * @version 1.0
 */
@Data
public class BasePage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(name = "排序字段", example = "id")
    private String sort = "id";

    @Schema(name = "排序方式", example = "desc")
    private String order = "desc";

    @Schema(name = "当前页", example = "1")
    private int page = 1;

    @Schema(name = "每页条数", example = "20")
    private int pageSize = 20;



    public Pageable toPage() {
        return PageRequest.of(
                page - 1,
                pageSize,
                Sort.by(Sort.Direction.fromOptionalString(order).orElse(Sort.Direction.DESC), sort)
        );

    }

    public Pageable toPage(Sort sort) {
        return PageRequest.of(
                page - 1,
                pageSize,
                sort
        );

    }
}
