plugins {
    id("java-library")
}

description = "jpa-camunda-component"

dependencies {
    api(project(":backend:components:camunda-component:api-camunda-component"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-crypto")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly(rootProject.libs.h2)
}
