plugins {
    id("java-library")
}

description = "embedded-engine-component"

dependencies {
    api(project(":backend:components:camunda-component:api-engine-component"))
    implementation("org.springframework:spring-context")

    // 嵌入式 Camunda 引擎全家桶（自 workflow-module 迁入）
    implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter:7.24.0")
    implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-webapp:7.24.0")
    implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-rest:7.24.0")
    implementation("org.camunda.bpm:camunda-engine-plugin-connect:7.24.0")
    implementation("org.camunda.connect:camunda-connect-http-client:7.24.0")
    implementation("org.camunda.bpm:camunda-engine-plugin-spin:7.24.0")
    implementation("org.camunda.spin:camunda-spin-dataformat-json-jackson:7.24.0")
    implementation("org.graalvm.polyglot:polyglot:24.2.1")
    implementation("org.graalvm.polyglot:js:24.2.1")
    implementation("org.graalvm.js:js-scriptengine:24.2.1")
    implementation("io.holunda.data:camunda-bpm-data:2026.04.2")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
