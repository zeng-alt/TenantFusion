package com.github.zeng.alt.camunda.engine.embedded.deploy;

import com.github.zeng.alt.camunda.engine.api.deploy.DeployBundleCommand;
import com.github.zeng.alt.camunda.engine.api.deploy.DeploymentApi;
import com.github.zeng.alt.camunda.engine.api.deploy.DeploymentInformation;
import com.github.zeng.alt.camunda.engine.api.deploy.NamedResource;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;

/**
 * 嵌入式部署实现
 *
 * @author zengAlt
 */
@Service
public class EmbeddedDeploymentApi implements DeploymentApi {

    private final RepositoryService repositoryService;

    public EmbeddedDeploymentApi(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public DeploymentInformation deploy(DeployBundleCommand command) {
        if (command.getResources() == null || command.getResources().isEmpty()) {
            throw new IllegalArgumentException("部署资源不能为空");
        }
        DeploymentBuilder builder = repositoryService.createDeployment();
        for (NamedResource resource : command.getResources()) {
            builder.addInputStream(resource.getName(), new ByteArrayInputStream(resource.getContent()));
        }
        if (StringUtils.hasText(command.getTenantId())) {
            builder.tenantId(command.getTenantId());
        }
        Deployment deployment = builder.deploy();
        return DeploymentInformation.builder()
                .deploymentId(deployment.getId())
                .name(deployment.getName())
                .build();
    }
}
