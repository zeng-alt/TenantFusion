package com.github.zeng.alt.doc;


import io.swagger.v3.oas.models.servers.Server;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class ServerBuilder {

    public List<Server> build(List<OpenApiServerProperties> list) {

        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        return list.stream()
                .map(this::build)
                .toList();
    }

    private Server build(OpenApiServerProperties p) {

        return new Server()
                .url(p.getUrl())
                .description(p.getDescription());
    }
}