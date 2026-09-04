plugins {
    id("java-library")
}

description = "liquibase-tenant-component"

dependencies {
    api(project(":backend:components:tenant-component:api-tenant-component"))
    api(project(":backend:components:tenant-component:core-tenant-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-jdbc")
    compileOnly("org.liquibase:liquibase-core")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.liquibase:liquibase-core")
    testImplementation(rootProject.libs.h2)
    testImplementation("org.postgresql:postgresql")
}
