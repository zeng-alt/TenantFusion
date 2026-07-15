package com.github.zeng.alt.config.server.controller;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.entity.ConfigAppEntity;
import com.github.zeng.alt.config.server.repository.ConfigAppRepository;
import com.github.zeng.alt.config.server.service.ConfigManageService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommonsLog
@RestController
@RequestMapping("/api/config")
public class ConfigClientController {

    private final ConfigManageService configManageService;
    private final ConfigAppRepository configAppRepository;

    public ConfigClientController(ConfigManageService configManageService,
                                  ConfigAppRepository configAppRepository) {
        this.configManageService = configManageService;
        this.configAppRepository = configAppRepository;
    }

    @GetMapping("/fetch")
    public RestResponse<List<ConfigItemDTO>> fetchConfigs(
            @RequestParam String appCode,
            @RequestParam(required = false) List<String> dataIds) {
        ConfigAppEntity app = configAppRepository.findByAppCode(appCode)
                .getOrElseThrow(() -> new IllegalArgumentException("App not found: " + appCode));

        List<ConfigItemDTO> result;
        if (dataIds != null && !dataIds.isEmpty()) {
            result = configManageService.getConfigs(app.getAppId(), dataIds);
        } else {
            result = configManageService.getAllConfigs(app.getAppId());
        }

        return RestResponse.success(result);
    }

    @GetMapping("/fetch/{dataId}")
    public RestResponse<ConfigItemDTO> fetchConfig(
            @RequestParam String appCode,
            @PathVariable String dataId,
            @RequestParam(defaultValue = "DEFAULT_GROUP") String group) {
        ConfigAppEntity app = configAppRepository.findByAppCode(appCode)
                .getOrElseThrow(() -> new IllegalArgumentException("App not found: " + appCode));

        ConfigItemDTO config = configManageService.toDTO(
                configManageService.getConfig(app.getAppId(), dataId, group));

        return RestResponse.success(config);
    }
}
