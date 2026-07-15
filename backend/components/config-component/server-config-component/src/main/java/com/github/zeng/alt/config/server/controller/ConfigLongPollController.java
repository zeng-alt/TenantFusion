package com.github.zeng.alt.config.server.controller;

import com.github.zeng.alt.config.server.listener.ConfigLongPollManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

@CommonsLog
@RestController
@RequestMapping("/api/config")
public class ConfigLongPollController {

    private final ConfigLongPollManager longPollManager;

    public ConfigLongPollController(ConfigLongPollManager longPollManager) {
        this.longPollManager = longPollManager;
    }

    @PostMapping("/listener")
    public DeferredResult<List<String>> listen(
            @RequestParam(defaultValue = "DEFAULT_GROUP") String appCode,
            @RequestParam long listenTimeoutMs,
            HttpServletRequest request) {
        DeferredResult<List<String>> deferredResult = new DeferredResult<>(listenTimeoutMs, List.of());
        longPollManager.register(appCode, deferredResult);
        log.debug("Long poll registered for appCode=" + appCode + ", timeout=" + listenTimeoutMs);
        return deferredResult;
    }
}
