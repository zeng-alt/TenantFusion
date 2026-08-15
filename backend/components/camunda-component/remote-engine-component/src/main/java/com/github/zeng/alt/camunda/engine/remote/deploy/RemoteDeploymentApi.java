package com.github.zeng.alt.camunda.engine.remote.deploy;

import com.github.zeng.alt.camunda.engine.api.deploy.DeployBundleCommand;
import com.github.zeng.alt.camunda.engine.api.deploy.DeploymentApi;
import com.github.zeng.alt.camunda.engine.api.deploy.DeploymentInformation;
import com.github.zeng.alt.camunda.engine.api.deploy.NamedResource;
import org.camunda.community.rest.client.api.DeploymentApiClient;
import org.camunda.community.rest.client.model.DeploymentWithDefinitionsDto;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 远程部署实现
 *
 * @author zengAlt
 */
@Service
public class RemoteDeploymentApi implements DeploymentApi {

    private final DeploymentApiClient deploymentApiClient;

    public RemoteDeploymentApi(DeploymentApiClient deploymentApiClient) {
        this.deploymentApiClient = deploymentApiClient;
    }

    @Override
    public DeploymentInformation deploy(DeployBundleCommand command) {
        if (command.getResources() == null || command.getResources().isEmpty()) {
            throw new IllegalArgumentException("部署资源不能为空");
        }
        List<MultipartFile> files = command.getResources().stream()
                .map(r -> (MultipartFile) new ByteArrayMultipartFile(r.getName(), r.getContent()))
                .toList();
        DeploymentWithDefinitionsDto deployment = deploymentApiClient
                .createDeployment("deployment", null, false, false, null, null,
                        files.toArray(new MultipartFile[0]))
                .getBody();
        if (deployment == null) {
            throw new IllegalStateException("部署失败");
        }
        return DeploymentInformation.builder()
                .deploymentId(deployment.getId())
                .name(deployment.getName())
                .build();
    }
}
