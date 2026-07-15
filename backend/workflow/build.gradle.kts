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
//    implementation(project(":backend:components:security-component:jwt-auth-security-component"))
    implementation(project(":backend:module:workflow-module"))
    // Camunda REST API 的 OpenAPI 定义
//    implementation("org.camunda.bpm:camunda-engine-rest-openapi:7.24.0")
    runtimeOnly(libs.h2)
//    implementation(project(":backend:components:doc-component"))
//    implementation("org.springframework.boot:spring-boot-starter-web")
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//    implementation(libs.spring.boot.starter.validation)
//    implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-webapp:7.24.0")
//    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
//    implementation("org.springframework.boot:spring-boot-starter-actuator")
//
//    compileOnly(libs.lombok)
//
//    annotationProcessor(libs.spring.boot.configuration.processor)
}

tasks.test {
    useJUnitPlatform()
}