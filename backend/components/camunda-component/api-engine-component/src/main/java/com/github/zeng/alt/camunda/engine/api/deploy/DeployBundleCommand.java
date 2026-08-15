package com.github.zeng.alt.camunda.engine.api.deploy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 部署命令
 *
 * @author zengAlt
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployBundleCommand implements Serializable {

    /**
     * 待部署的资源列表
     */
    private List<NamedResource> resources;

    /**
     * 租户ID（可选）
     */
    private String tenantId;
}
