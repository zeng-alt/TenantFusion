plugins {
    id("java-library")
}

description = "remote-camunda-component"

dependencies {
    api(project(":backend:components:camunda-component:api-camunda-component"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-webflux")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
