package com.github.zeng.alt.doc;

import lombok.Data;

@Data
public class OpenApiServerProperties {

    /**
     * 服务地址
     */
    private String url;

    /**
     * 描述
     */
    private String description;
}