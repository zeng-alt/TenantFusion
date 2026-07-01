package com.github.zeng.alt.doc;

import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.Data;

@Data
public class SecuritySchemeProperties {

    private SecurityScheme.Type type;

    private String scheme;

    private String bearerFormat;

    private String description;

    private String name;

    private SecurityScheme.In in;
}