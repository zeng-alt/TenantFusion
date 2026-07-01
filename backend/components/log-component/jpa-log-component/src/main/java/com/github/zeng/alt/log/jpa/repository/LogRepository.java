package com.github.zeng.alt.log.jpa.repository;

import com.github.zeng.alt.log.jpa.entity.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 操作日志 JPA Repository。
 *
 * @author zengJiaJun
 * @since 2026-07-01
 * @version 1.0
 */
@Repository
public interface LogRepository extends JpaRepository<LogEntity, Long> {

    /**
     * 清除指定时间之前的日志。
     *
     * @param time 时间
     */
    void deleteByOperTimeBefore(java.time.LocalDateTime time);
}
