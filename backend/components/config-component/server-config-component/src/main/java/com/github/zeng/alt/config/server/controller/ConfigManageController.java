package com.github.zeng.alt.config.server.controller;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.config.storage.model.ConfigAppDto;
import com.github.zeng.alt.config.storage.model.ConfigEntryDto;
import com.github.zeng.alt.config.storage.model.ConfigReleaseDto;
import com.github.zeng.alt.config.server.model.ConfigAppCmd;
import com.github.zeng.alt.config.server.model.ConfigCreateCmd;
import com.github.zeng.alt.config.server.model.ConfigPublishCmd;
import com.github.zeng.alt.config.server.model.ConfigRollbackCmd;
import com.github.zeng.alt.config.server.model.ConfigUpdateCmd;
import com.github.zeng.alt.config.server.service.ConfigManageService;
import com.github.zeng.alt.config.server.service.ConfigPublishService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/admin")
@RequiredArgsConstructor
public class ConfigManageController {
    private final ConfigManageService manage;
    private final ConfigPublishService publish;

    @PostMapping("/app")
    public RestResponse<ConfigAppDto> createApp(@Valid @RequestBody ConfigAppCmd command) {
        return manage.createApp(command).fold(RestResponse::fail, RestResponse::success);
    }

    @GetMapping("/app")
    public RestResponse<List<ConfigAppDto>> listApps() {
        return RestResponse.success(manage.listApps());
    }

    @GetMapping("/app/{appCode}")
    public RestResponse<ConfigAppDto> getApp(@PathVariable String appCode) {
        return manage.getApp(appCode).fold(RestResponse::fail, RestResponse::success);
    }

    @PostMapping("/config")
    public RestResponse<ConfigEntryDto> createConfig(@Valid @RequestBody ConfigCreateCmd command) {
        return manage.create(command).fold(RestResponse::fail, RestResponse::success);
    }

    @PutMapping("/config/{configId}")
    public RestResponse<ConfigEntryDto> updateConfig(
        @PathVariable Long configId,
        @Valid @RequestBody ConfigUpdateCmd command
    ) {
        return manage.update(configId, command).fold(RestResponse::fail, RestResponse::success);
    }

    @DeleteMapping("/config/{configId}")
    public RestResponse<Long> deleteConfig(@PathVariable Long configId) {
        return manage.delete(configId).fold(RestResponse::fail, RestResponse::success);
    }

    @GetMapping("/config/list/{appCode}")
    public RestResponse<List<ConfigEntryDto>> listConfigs(@PathVariable String appCode) {
        return manage.getApp(appCode).map(app -> manage.listByAppId(app.appId()))
                .fold(RestResponse::fail, RestResponse::success);
    }

    @PostMapping("/publish")
    public RestResponse<ConfigReleaseDto> publish(@Valid @RequestBody ConfigPublishCmd command) {
        return publish.publish(command).fold(RestResponse::fail, RestResponse::success);
    }

    @PostMapping("/rollback")
    public RestResponse<ConfigReleaseDto> rollback(@Valid @RequestBody ConfigRollbackCmd command) {
        return publish.rollback(command).fold(RestResponse::fail, RestResponse::success);
    }
}
