package com.github.zeng.alt.admin.interfaces.rest;

import com.github.zeng.alt.api.rest.PageRestResponse;
import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.log.jpa.entity.LoginLogEntity;
import com.github.zeng.alt.log.jpa.entity.QLoginLogEntity;
import com.github.zeng.alt.log.jpa.repository.LoginLogRepository;
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
 * 登录日志查询接口。
 *
 * @author zengJiaJun
 * @since 2026-08-12
 * @version 1.0
 */
@Tag(name = "登录日志接口")
@RestController
@RequestMapping("/v1/log/login")
@RequiredArgsConstructor
public class LoginLogController {

    private final LoginLogRepository loginLogRepository;

    @Operation(summary = "分页查询登录日志")
    @GetMapping("/list")
    public PageRestResponse<LoginLogEntity> list(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String status) {
        QLoginLogEntity loginLog = QLoginLogEntity.loginLogEntity;
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(username)) {
            builder.and(loginLog.username.contains(username));
        }
        if (StringUtils.hasText(ip)) {
            builder.and(loginLog.ip.contains(ip));
        }
        if (StringUtils.hasText(status)) {
            builder.and(loginLog.status.eq(status));
        }
        Predicate predicate = builder.getValue();
        Page<LoginLogEntity> page = predicate == null
                ? loginLogRepository.findAll(PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "loginTime")))
                : loginLogRepository.findAll(predicate,
                        PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "loginTime")));
        return PageRestResponse.of(page.getContent(), page.getTotalElements(), pageSize, pageNo);
    }

    @Operation(summary = "获取登录日志详情")
    @GetMapping("/{id}")
    public RestResponse<LoginLogEntity> detail(@PathVariable Long id) {
        return RestResponse.success(loginLogRepository.findById(id).getOrNull());
    }
}
