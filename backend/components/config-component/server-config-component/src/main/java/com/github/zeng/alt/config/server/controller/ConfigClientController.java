package com.github.zeng.alt.config.server.controller;

import com.github.zeng.alt.api.rest.RestResponse;
import com.github.zeng.alt.config.ConfigTemplate;
import com.github.zeng.alt.config.model.ConfigItemDTO;
import com.github.zeng.alt.config.server.service.ConfigPublishService;
import io.vavr.collection.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigClientController {
    private final ConfigPublishService publish;

    @GetMapping("/fetch")
    public RestResponse<java.util.List<ConfigItemDTO>> fetchConfigs(
        @RequestParam String appCode,
        @RequestParam(required = false) java.util.List<String> dataIds
    ) {
        return publish.getPublishedConfigs(appCode)
                .map(items -> items.stream().filter(item -> dataIds == null || dataIds.isEmpty()
                        || dataIds.contains(item.getDataId())).toList())
                .fold(RestResponse::fail, RestResponse::success);
    }

    @GetMapping("/fetch/{dataId}")
    public RestResponse<ConfigItemDTO> fetchConfig(
        @RequestParam String appCode,
        @PathVariable String dataId,
        @RequestParam(defaultValue = ConfigTemplate.DEFAULT_GROUP) String group
    ) {
        return publish.getPublishedConfigs(appCode).flatMap(items -> List.ofAll(items)
                .find(item -> dataId.equals(item.getDataId()) && group.equals(item.getGroup()))
                .toEither("已发布配置不存在: " + dataId)).fold(RestResponse::fail, RestResponse::success);
    }
}
