plugins {
    id("java-library")
}

description = "core security"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("io.projectreactor:reactor-core")
    api("jakarta.servlet:jakarta.servlet-api")
//    implementation("org.springframework:spring-webmvc")
//    implementation("org.springframework:spring-reactive")
    api("org.springframework.boot:spring-boot-starter-security")
    api(project(":backend:components:security-component:api-security-component"))
    api(project(":backend:components:json-component"))
}