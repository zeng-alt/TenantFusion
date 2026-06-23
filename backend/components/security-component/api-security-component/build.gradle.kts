plugins {
    id("java-library")
}

description = "security api"

dependencies {
    implementation("org.springframework:spring-web")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation(project(":backend:components:json-component"))
    api(project(":backend:components:storage-component:api-storage-component"))
    api(project(":backend:components:tenant-component:api-tenant-component"))
}