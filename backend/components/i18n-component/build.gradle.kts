plugins {
    id("java-library")
    alias(libs.plugins.spring.dependency.management)
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"
description = "国际化组件（支持文件/数据库两种模式）"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.13")
    }
}

dependencies {
    api(project(":backend:components:api-component"))
    api(project(":backend:components:domain-component"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework:spring-context")

    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")

    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    // 测试 - 统一使用 Spring Boot 自带的 JUnit 管理
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(rootProject.libs.h2)
    testCompileOnly(rootProject.libs.lombok)
    testAnnotationProcessor(rootProject.libs.lombok)
}

tasks.test {
    useJUnitPlatform()
}