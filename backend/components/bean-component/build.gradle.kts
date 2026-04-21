plugins {
    id("java-library")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"
description = "spring bean工具包"

dependencies {
    api(project(":backend:components:api-component"))
    api(rootProject.libs.vavr)
}

