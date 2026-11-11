plugins {
    id("java-library")
}

description = "local-tenant-component"

dependencies {
    api(project(":backend:components:tenant-component:core-tenant-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-jdbc")
    compileOnly("org.springframework:spring-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework.security:spring-security-core")
    compileOnly("org.springframework.security:spring-security-web")
    compileOnly(project(":backend:components:security-component:api-security-component"))
    compileOnly(project(":backend:components:security-component:core-security-component"))
    compileOnly("com.zaxxer:HikariCP")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation("org.springframework.security:spring-security-web")
    testImplementation(project(":backend:components:security-component:api-security-component"))
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("com.zaxxer:HikariCP")
    testImplementation(rootProject.libs.h2)
}
