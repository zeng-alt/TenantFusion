package com.github.zeng.alt.doc;

import lombok.Data;

@Data
public class LicenseProperties {

    private String name;

    private String url;

    /**
     * SPDX Identifier(OpenAPI 3.1)
     */
    private String identifier;
}