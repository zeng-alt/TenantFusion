package com.github.zeng.alt.camunda.engine.api.correlation;

/**
 * 消息关联 API
 * <p>
 * 参考 dev.bpm-crafters.process-engine-api 的 CorrelationApi 设计。
 *
 * @author zengAlt
 */
public interface CorrelationApi {

    /**
     * 关联消息到流程实例
     */
    void correlateMessage(CorrelateMessageCmd cmd);
}
