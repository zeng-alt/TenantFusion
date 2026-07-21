plugins {
    id("java-library")
}

description = "rbac serve security"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("io.projectreactor:reactor-core")
    api("jakarta.servlet:jakarta.servlet-api")
    api(project(":backend:components:security-component:core-security-component"))
    api(project(":backend:components:json-component"))
    api(project(":backend:components:storage-component:api-storage-component"))
    api(project(":backend:components:message-component:api-message-component"))

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}