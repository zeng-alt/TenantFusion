plugins {
    id("java-library")
}

description = "工作流应用模块"

dependencies {
    api(project(":backend:components:camunda-component"))
    api(project(":backend:components:doc-component"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api(libs.spring.boot.starter.validation)
    api("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-webapp:7.24.0")
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    api("org.springframework.boot:spring-boot-starter-actuator")

    compileOnly(libs.lombok)

    annotationProcessor(libs.spring.boot.configuration.processor)
}
