package com.github.zeng.alt.log.jpa.service;

import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.core.model.LoginClientDto;
import com.github.zeng.alt.log.core.support.IpLocationResolver;
import com.github.zeng.alt.log.core.support.UserAgentResolver;
import com.github.zeng.alt.log.jpa.entity.LoginLogEntity;
import com.github.zeng.alt.log.jpa.mapper.JpaLogMapper;
import com.github.zeng.alt.log.jpa.repository.LogRepository;
import com.github.zeng.alt.log.jpa.repository.LoginLogRepository;
import io.vavr.control.Option;
import io.vavr.control.Try;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 使用 REQUIRES_NEW 确保业务回滚后仍保留操作与登录记录。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@CommonsLog
@RequiredArgsConstructor
public class LogPersistenceServiceImpl implements LogPersistenceService {
    private final LogRepository operations;
    private final LoginLogRepository logins;
    private final JpaLogMapper mapper;
    private final IpLocationResolver locations;
    private final UserAgentResolver userAgents;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void create(OperLogEvent event) {
        operations.save(mapper.toEntity(event));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void create(LoginInfoEvent event) {
        final LoginLogEntity entity = mapper.toEntity(event, Option.of(event.getLoginTime()).getOrElse(LocalDateTime::now));
        if (!StringUtils.hasText(entity.getLoginLocation())) {
            entity.setLoginLocation(Try.of(() -> locations.resolve(event.getIp()))
                    .onFailure(error -> log.warn("Failed to resolve login location", error))
                    .getOrElse(Option.none()).getOrElse(""));
        }
        enrichClient(entity, event.getUserAgent());
        logins.save(entity);
    }

    private void enrichClient(LoginLogEntity entity, String userAgent) {
        if (StringUtils.hasText(entity.getBrowser()) && StringUtils.hasText(entity.getOs())) {
            return;
        }
        final LoginClientDto client = Try.of(() -> userAgents.resolve(userAgent))
                .onFailure(error -> log.warn("Failed to resolve login client", error))
                .getOrElse(new LoginClientDto("", ""));
        if (!StringUtils.hasText(entity.getBrowser())) {
            entity.setBrowser(client.browser());
        }
        if (!StringUtils.hasText(entity.getOs())) {
            entity.setOs(client.os());
        }
    }
}
