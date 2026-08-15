plugins {
    id("java-library")
}

description = "工作流应用模块"

dependencies {
    api(project(":backend:components:security-component:api-security-component"))
    api(project(":backend:components:api-component"))
    api(project(":backend:components:json-component"))
    api(project(":backend:components:domain-component"))
    api(project(":backend:components:camunda-component:api-engine-component"))
    api(project(":backend:components:doc-component"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api(libs.spring.boot.starter.validation)
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    api("org.springframework.boot:spring-boot-starter-actuator")

    api(project(":backend:components:security-component:core-security-component"))

    // 纯 BPMN 模型解析（无引擎依赖，用于表单定义解析，本地/远程通用）
    api("org.camunda.bpm.model:camunda-bpmn-model:7.24.0")

    annotationProcessor(libs.spring.boot.configuration.processor)
    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }

    compileOnly(libs.lombok)
    compileOnly(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    // 解决 Lombok 与 MapStruct 注解处理顺序问题
    annotationProcessor(libs.lombok.mapstruct.binding)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
