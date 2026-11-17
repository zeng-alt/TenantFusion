package com.github.zeng.alt.log.jpa.service;

import com.github.zeng.alt.log.LoginInfoEvent;
import com.github.zeng.alt.log.OperLogEvent;

/**
 * 在独立事务中保存日志，不参与业务事务的提交或回滚。
 *
 * @author zeng-alt
 * @since 0.0.1-SNAPSHOT
 */
public interface LogPersistenceService {
    void create(OperLogEvent event);

    void create(LoginInfoEvent event);
}
