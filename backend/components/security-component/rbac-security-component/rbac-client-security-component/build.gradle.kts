plugins {
    id("java-library")
}

description = "rbac client security"

dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("io.projectreactor:reactor-core")
    api("jakarta.servlet:jakarta.servlet-api")
    api(project(":backend:components:security-component:core-security-component"))
    api(project(":backend:components:json-component"))
    api(project(":backend:components:event-component:api-event-component"))

    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-actuator")
    compileOnly(project(":backend:components:security-component:rbac-security-component:rbac-serve-security-component"))

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
