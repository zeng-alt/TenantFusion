package com.github.zeng.alt.camunda.engine.api.deploy;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 部署结果信息
 *
 * @author zengAlt
 */
@Data
@Builder
public class DeploymentInformation implements Serializable {

    /**
     * 部署ID
     */
    private String deploymentId;

    /**
     * 部署名称
     */
    private String name;
}
