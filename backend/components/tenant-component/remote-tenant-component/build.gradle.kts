plugins {
    id("java-library")
}

description = "remote-tenant-component"

dependencies {
    api(project(":backend:components:tenant-component:core-tenant-component"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // 与 remote-engine-component 同一套 OpenFeign（Boot 3.5 对应 4.3.x，外层已有 catalog 时可收敛过去）
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.3.3")
    implementation("org.springframework:spring-web")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
}
