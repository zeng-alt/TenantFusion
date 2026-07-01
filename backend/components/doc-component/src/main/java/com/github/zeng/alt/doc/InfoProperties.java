package com.github.zeng.alt.doc;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
public class InfoProperties {

    private String title;

    private String description;

    private String version;

    @NestedConfigurationProperty
    private ContactProperties contact;

    @NestedConfigurationProperty
    private LicenseProperties license;
}