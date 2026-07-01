package com.github.zeng.alt.doc;

import lombok.Data;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "springdoc")
public class SpringDocProperties {

    @NestedConfigurationProperty
    private InfoProperties info = new InfoProperties();

    @NestedConfigurationProperty
    private ComponentsProperties components = new ComponentsProperties();

    private List<TagProperties> tags = new ArrayList<>();

    @NestedConfigurationProperty
    private ExternalDocsProperties externalDocs;

    private List<ServerProperties> servers = new ArrayList<>();
}