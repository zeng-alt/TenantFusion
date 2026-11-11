plugins {
    id("java-library")
    jacoco
}

description = "ABAC 核心授权流程、扩展 SPI 与公共自动配置"

dependencies {
    api(project(":backend:components:security-component:abac-security-component:api-abac-security-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
