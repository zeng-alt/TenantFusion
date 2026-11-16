package com.github.zeng.alt.config.server.controller;

import com.github.zeng.alt.config.server.listener.ConfigLongPollManager;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigLongPollController {
    private final ConfigLongPollManager longPollManager;

    @PostMapping("/listener")
    public DeferredResult<List<String>> listen(
        @NotBlank @RequestParam String appCode,
        @Min(1000) @Max(60000) @RequestParam long listenTimeoutMs
    ) {
        final var deferred = new DeferredResult<List<String>>(listenTimeoutMs, List.of());
        longPollManager.register(appCode, deferred);
        return deferred;
    }
}
