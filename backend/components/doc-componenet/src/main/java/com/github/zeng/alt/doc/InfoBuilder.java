package com.github.zeng.alt.doc;


import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;


public class InfoBuilder {

    public Info build(InfoProperties p) {

        return new Info()
                .title(p.getTitle())
                .description(p.getDescription())
                .version(p.getVersion())
                .contact(buildContact(p.getContact()))
                .license(buildLicense(p.getLicense()));
    }

    private Contact buildContact(ContactProperties p) {

        if (p == null) {
            return null;
        }

        return new Contact()
                .name(p.getName())
                .email(p.getEmail())
                .url(p.getUrl());
    }

    private License buildLicense(LicenseProperties p) {

        if (p == null) {
            return null;
        }

        return new License()
                .name(p.getName())
                .url(p.getUrl())
                .identifier(p.getIdentifier());
    }

}