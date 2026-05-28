group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

plugins {
    id("java-library")
}

dependencies {
    api(project(":backend:components:core-component"))
    api(libs.spring.boot.starter.data.jpa)
    api(libs.querydsl.apt)
    api(libs.querydsl.jpa)
}
