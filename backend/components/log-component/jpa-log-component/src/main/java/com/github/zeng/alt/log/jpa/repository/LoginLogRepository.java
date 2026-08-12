package com.github.zeng.alt.log.jpa.repository;

import com.github.zeng.alt.domain.base.BaseRepository;
import com.github.zeng.alt.log.jpa.entity.LoginLogEntity;
import org.springframework.stereotype.Repository;

/**
 * 登录日志 JPA Repository。
 *
 * @author zengJiaJun
 * @since 2026-08-12
 * @version 1.0
 */
@Repository
public interface LoginLogRepository extends BaseRepository<LoginLogEntity, Long> {

    /**
     * 清除指定时间之前的登录日志。
     *
     * @param time 时间
     */
    void deleteByLoginTimeBefore(java.time.LocalDateTime time);
}
