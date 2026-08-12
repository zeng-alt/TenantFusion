package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.log.jpa.entity.LogEntity;
import com.github.zeng.alt.log.jpa.entity.QLogEntity;
import com.github.zeng.alt.log.jpa.repository.LogRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口。
 *
 * @author zengJiaJun
 * @since 2026-08-12
 * @version 1.0
 */
@Tag(name = "操作日志接口")
@RestController
@RequestMapping("/v1/log/oper")
@RequiredArgsConstructor
public class OperLogController {

    private final LogRepository logRepository;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/list")
    public PageRestResponse<LogEntity> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer businessType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String operName,
            @RequestParam(required = false) String operIp) {
        QLogEntity log = QLogEntity.logEntity;
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(title)) {
            builder.and(log.title.contains(title));
        }
        if (businessType != null) {
            builder.and(log.businessType.eq(businessType));
        }
        if (status != null) {
            builder.and(log.status.eq(status));
        }
        if (StringUtils.hasText(operName)) {
            builder.and(log.operName.contains(operName));
        }
        if (StringUtils.hasText(operIp)) {
            builder.and(log.operIp.contains(operIp));
        }
        Predicate predicate = builder.getValue();
        Page<LogEntity> page = predicate == null
                ? logRepository.findAll(PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "operTime")))
                : logRepository.findAll(predicate,
                        PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "operTime")));
        return PageRestResponse.of(page.getContent(), page.getTotalElements(), pageSize, pageNo);
    }

    @Operation(summary = "获取操作日志详情")
    @GetMapping("/{id}")
    public RestResponse<LogEntity> detail(@PathVariable Long id) {
        return RestResponse.success(logRepository.findById(id).getOrNull());
    }
}
