package com.github.zeng.alt.camunda.engine.api.correlation;

/**
 * 信号发送 API
 * <p>
 * 参考 dev.bpm-crafters.process-engine-api 的 SignalApi 设计。
 *
 * @author zengAlt
 */
public interface SignalApi {

    /**
     * 发送信号
     */
    void send(SendSignalCmd cmd);
}
