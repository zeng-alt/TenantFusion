plugins {
    id("java-library")
}

description = "rbac client security"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("io.projectreactor:reactor-core")
    api("jakarta.servlet:jakarta.servlet-api")
    api(project(":backend:components:security-component:core-security-component"))
    api(project(":backend:components:json-component"))
    api(project(":backend:components:message-component:api-message-component"))

    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-actuator")
    compileOnly(project(":backend:components:security-component:rbac-security-component:rbac-serve-security-component"))

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
