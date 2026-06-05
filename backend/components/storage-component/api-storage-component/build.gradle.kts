plugins {
    id("java-library")
}

description = "缓存api"

dependencies {
    implementation(project(":backend:components:api-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
