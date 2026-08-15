package com.github.zeng.alt.camunda.engine.api.deploy;

/**
 * 部署 API
 * <p>
 * 参考 dev.bpm-crafters.process-engine-api 的 DeploymentApi 设计。
 *
 * @author zengAlt
 */
public interface DeploymentApi {

    /**
     * 部署资源
     */
    DeploymentInformation deploy(DeployBundleCommand command);
}
