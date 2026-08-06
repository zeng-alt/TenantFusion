plugins {
    id("java-library")
}

description = "工作流应用模块"

dependencies {
    api(project(":backend:components:api-component"))
    api(project(":backend:components:json-component"))
    api(project(":backend:components:domain-component"))
    api(project(":backend:components:camunda-component"))
    api(project(":backend:components:doc-component"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api(libs.spring.boot.starter.validation)
    api("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-webapp:7.24.0")
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    api("org.springframework.boot:spring-boot-starter-actuator")



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
