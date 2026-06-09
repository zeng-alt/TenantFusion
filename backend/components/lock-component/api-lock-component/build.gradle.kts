plugins {
    id("java-library")
}

description = "api-lock-component"

dependencies {
    implementation(project(":backend:components:api-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(rootProject.libs.h2)
}
