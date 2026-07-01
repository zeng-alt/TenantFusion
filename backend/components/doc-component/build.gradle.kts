plugins {
    id("java-library")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"
description = "doc文档组件"


dependencies {
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api")
    implementation("commons-io:commons-io:2.16.1")
}

