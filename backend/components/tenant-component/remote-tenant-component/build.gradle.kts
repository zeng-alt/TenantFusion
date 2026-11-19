plugins {
    id("java-library")
}

description = "remote-tenant-component"

dependencies {
    implementation("org.springframework.boot:spring-boot-http-converter")
    implementation("org.springframework.boot:spring-boot-jackson")
    api(project(":backend:components:tenant-component:core-tenant-component"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // OpenFeign 版本由根项目 Spring Cloud BOM 统一管理。
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework:spring-web")
    implementation("tools.jackson.core:jackson-databind")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
}
