plugins {
    id("java-library")
}

description = "api-message-component"

dependencies {
    implementation(project(":backend:components:api-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":backend:components:json-component"))
    implementation("org.springframework:spring-tx")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
