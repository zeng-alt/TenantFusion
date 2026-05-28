

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"
description = "REST API 注解处理器模块，编译期通过 JavaPoet 生成 Spring Functional CRUD 代码"

plugins {
    id("java-library")
}

dependencies {
    api(project(":backend:components:core-component"))
    api(libs.spring.boot.starter.data.jpa)
    api(libs.querydsl.apt)
    api(libs.querydsl.jpa)

    // JavaPoet - 代码生成
    api("com.squareup:javapoet:1.13.0")

    // 注解处理器 API（JDK 内置，但显式声明以支持 IDE）
    compileOnly("java.compiler:java.compiler:9")
}

// 告知编译器该模块包含注解处理器
tasks.withType<JavaCompile> {
    options.annotationProcessorPath = configurations.annotationProcessor.getOrElse(configurations.compileOnly.get())
}