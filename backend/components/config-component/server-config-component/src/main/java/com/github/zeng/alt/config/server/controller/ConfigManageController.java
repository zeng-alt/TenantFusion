package com.github.zeng.alt.config.server.controller;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.entity.ConfigAppEntity;
import com.github.zeng.alt.config.server.entity.ConfigInfoEntity;
import com.github.zeng.alt.config.server.repository.ConfigAppRepository;
import com.github.zeng.alt.config.server.service.ConfigManageService;
import com.github.zeng.alt.config.server.service.ConfigPublishService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CommonsLog
@RestController
@RequestMapping("/api/config/admin")
public class ConfigManageController {

    private final ConfigManageService configManageService;
    private final ConfigPublishService configPublishService;
    private final ConfigAppRepository configAppRepository;

    public ConfigManageController(ConfigManageService configManageService,
                                  ConfigPublishService configPublishService,
                                  ConfigAppRepository configAppRepository) {
        this.configManageService = configManageService;
        this.configPublishService = configPublishService;
        this.configAppRepository = configAppRepository;
    }

    @PostMapping("/app")
    public RestResponse<ConfigAppEntity> createApp(@RequestBody ConfigAppEntity app) {
        ConfigAppEntity saved = configAppRepository.save(app);
        log.info("Created config app: " + saved.getAppCode());
        return RestResponse.success(saved);
    }

    @GetMapping("/app")
    public RestResponse<List<ConfigAppEntity>> listApps() {
        return RestResponse.success(configAppRepository.findAll());
    }

    @GetMapping("/app/{appCode}")
    public RestResponse<ConfigAppEntity> getApp(@PathVariable String appCode) {
        return RestResponse.success(
                configAppRepository.findByAppCode(appCode)
                        .getOrElseThrow(() -> new IllegalArgumentException("App not found: " + appCode)));
    }

    @PostMapping("/config")
    public RestResponse<ConfigInfoEntity> createConfig(@RequestBody Map<String, Object> body) {
        String appCode = (String) body.get("appCode");
        ConfigAppEntity app = configAppRepository.findByAppCode(appCode)
                .getOrElseThrow(() -> new IllegalArgumentException("App not found: " + appCode));

        ConfigInfoEntity entity = configManageService.createConfig(
                app.getAppId(),
                (String) body.get("dataId"),
                (String) body.get("group"),
                (String) body.get("content"),
                (String) body.get("format"),
                (String) body.get("description"));
        return RestResponse.success(entity);
    }

    @PutMapping("/config/{configId}")
    public RestResponse<ConfigInfoEntity> updateConfig(@PathVariable Long configId,
                                                       @RequestBody Map<String, Object> body) {
        ConfigInfoEntity entity = configManageService.updateConfig(
                configId,
                (String) body.get("content"),
                (String) body.get("description"),
                (String) body.get("operator"));
        return RestResponse.success(entity);
    }

    @GetMapping("/config/list/{appCode}")
    public RestResponse<List<ConfigInfoEntity>> listConfigs(@PathVariable String appCode) {
        ConfigAppEntity app = configAppRepository.findByAppCode(appCode)
                .getOrElseThrow(() -> new IllegalArgumentException("App not found: " + appCode));
        return RestResponse.success(configManageService.listByAppId(app.getAppId()));
    }

    @PostMapping("/publish")
    public RestResponse<?> publish(@RequestBody Map<String, Object> body) {
        String appCode = (String) body.get("appCode");
        @SuppressWarnings("unchecked")
        List<Integer> configIdsRaw = (List<Integer>) body.get("configIds");
        List<Long> configIds = configIdsRaw.stream()
                .map(Integer::longValue)
                .collect(java.util.stream.Collectors.toList());
        String releaseNote = (String) body.get("releaseNote");

        ConfigAppEntity app = configAppRepository.findByAppCode(appCode)
                .getOrElseThrow(() -> new IllegalArgumentException("App not found: " + appCode));

        return RestResponse.success(
                configPublishService.publish(app.getAppId(), configIds, releaseNote));
    }
}
