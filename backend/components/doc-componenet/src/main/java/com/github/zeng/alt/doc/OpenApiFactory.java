package com.github.zeng.alt.doc;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.stereotype.Component;

@Component
public class OpenApiFactory {

    private final InfoBuilder infoBuilder;

    private final ComponentsBuilder componentsBuilder;

    private final TagBuilder tagBuilder;

    public OpenApiFactory() {
        this.infoBuilder = new InfoBuilder();
        this.componentsBuilder = new ComponentsBuilder();
        this.tagBuilder = new TagBuilder();
    }


    public OpenAPI build(SpringDocProperties p) {

        OpenAPI openApi = new OpenAPI();

        openApi.info(infoBuilder.build(p.getInfo()));

        openApi.tags(tagBuilder.build(p.getTags()));

        Components components =
                componentsBuilder.build(p.getComponents());

        openApi.components(components);

        addSecurity(openApi, components);

        return openApi;
    }

    private void addSecurity(OpenAPI openApi,
                             Components components) {

        if (components.getSecuritySchemes() == null) {
            return;
        }

        SecurityRequirement requirement =
                new SecurityRequirement();

        components.getSecuritySchemes()
                .keySet()
                .forEach(requirement::addList);

        openApi.addSecurityItem(requirement);
    }

}