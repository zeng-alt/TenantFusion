plugins {
    id("java-library")
    id("java-test-fixtures")
}

description = "api-event-component"

dependencies {
    api(project(":backend:components:api-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    api(project(":backend:components:json-component"))
    api("org.springframework:spring-messaging")
    implementation("org.springframework:spring-tx")
    api("org.springframework:spring-context")
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjweaver")
    api(project(":backend:components:tenant-component:api-tenant-component"))
    implementation(project(":backend:components:security-component:api-security-component"))
    implementation("org.springframework.security:spring-security-core")
    compileOnly(rootProject.libs.namastack.outbox.api)
    compileOnly(rootProject.libs.namastack.outbox.core)
    compileOnly(rootProject.libs.spring.modulith.events.core)

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation(project(":backend:components:security-component:api-security-component"))
    testFixturesImplementation("org.springframework.security:spring-security-core")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(rootProject.libs.namastack.outbox.api)
    testImplementation(rootProject.libs.namastack.outbox.core)
    testImplementation(rootProject.libs.spring.modulith.events.core)
    testImplementation(rootProject.libs.spring.modulith.starter.namastack)
    testImplementation(rootProject.libs.spring.modulith.starter.jpa)
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testRuntimeOnly(rootProject.libs.h2)
}
