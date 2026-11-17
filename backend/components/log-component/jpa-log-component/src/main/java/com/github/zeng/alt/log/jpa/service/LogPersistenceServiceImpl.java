package com.github.zeng.alt.log.jpa.service;

import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.log.OperLogEvent;
import com.github.zeng.alt.log.jpa.mapper.JpaLogMapper;
import com.github.zeng.alt.log.jpa.repository.LogRepository;
import com.github.zeng.alt.log.jpa.repository.LoginLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 使用 REQUIRES_NEW 确保业务回滚后仍保留操作与登录记录。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
@RequiredArgsConstructor
public class LogPersistenceServiceImpl implements LogPersistenceService {
    private final LogRepository operations;
    private final LoginLogRepository logins;
    private final JpaLogMapper mapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void create(OperLogEvent event) {
        operations.save(mapper.toEntity(event));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void create(LoginInfoEvent event) {
        logins.save(mapper.toEntity(event, LocalDateTime.now()));
    }
}
