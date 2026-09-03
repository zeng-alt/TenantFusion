plugins {
    id("java-library")
}

description = "tenant api"

dependencies {
    api(project(":backend:components:api-component"))
    api("org.springframework:spring-core")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
