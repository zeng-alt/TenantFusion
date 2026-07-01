package com.github.zeng.alt.doc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;

public class ComponentsBuilder {

    public Components build(ComponentsProperties properties) {

        Components components = new Components();

        properties.getSecuritySchemes()
                .forEach((name, item) ->
                        components.addSecuritySchemes(
                                name,
                                build(item)));

        return components;
    }

    private SecurityScheme build(SecuritySchemeProperties p) {

        SecurityScheme scheme = new SecurityScheme();

        scheme.setDescription(p.getDescription());

        scheme.setBearerFormat(p.getBearerFormat());

        scheme.setScheme(p.getScheme());

        scheme.setName(p.getName());

        scheme.setIn(p.getIn());

        scheme.setType(p.getType());

        return scheme;
    }

}