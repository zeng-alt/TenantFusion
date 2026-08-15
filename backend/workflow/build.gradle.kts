description = "admin-application"

plugins {
    id("java")
    id("org.springframework.boot")
    id("org.hibernate.orm")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    outputs.upToDateWhen { false }
}

dependencies {
    // Web管理平台
    implementation(project(":backend:components:security-component:jwt-auth-security-component"))
    implementation(project(":backend:components:storage-component:redisson-storage-component"))
    implementation(project(":backend:module:workflow-module"))
    implementation(project(":backend:components:camunda-component:remote-camunda-component"))
    // 嵌入式引擎实现（远程部署时替换为 remote-engine-component）
    implementation(project(":backend:components:camunda-component:embedded-engine-component"))
    // 引擎 API（编译 formkService 等 delegate 表达式 Bean 需要，运行时由 embedded-engine-component 提供）
    implementation("org.camunda.bpm:camunda-engine:7.24.0")
    // 流程变量读写框架（formkService 读取 IO 映射输入参数）
    implementation("io.holunda.data:camunda-bpm-data:2026.04.2")
    // Camunda REST API 的 OpenAPI 定义
//    implementation("org.camunda.bpm:camunda-engine-rest-openapi:7.24.0")
    runtimeOnly(libs.h2)
    implementation("org.springframework.boot:spring-boot-starter-web")
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//    implementation(libs.spring.boot.starter.validation)
//    implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-webapp:7.24.0")
//    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
//    implementation("org.springframework.boot:spring-boot-starter-actuator")
//
    compileOnly(libs.lombok)
//
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}