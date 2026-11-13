plugins {
    id("java-library")
}

description = "api-event-component"

dependencies {
    implementation(project(":backend:components:api-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":backend:components:json-component"))
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjweaver")
    api(project(":backend:components:tenant-component:api-tenant-component"))
    implementation(project(":backend:components:security-component:api-security-component"))
    implementation("org.springframework.security:spring-security-core")
    compileOnly(rootProject.libs.namastack.outbox.api)
    compileOnly(rootProject.libs.namastack.outbox.core)
    compileOnly(rootProject.libs.spring.modulith.events.core)

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(rootProject.libs.namastack.outbox.api)
    testImplementation(rootProject.libs.namastack.outbox.core)
    testImplementation(rootProject.libs.spring.modulith.events.core)
    testImplementation(rootProject.libs.spring.modulith.starter.namastack)
    testImplementation(rootProject.libs.spring.modulith.starter.jpa)
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testRuntimeOnly(rootProject.libs.h2)
}
