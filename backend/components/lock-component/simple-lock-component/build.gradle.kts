plugins {
    id("java-library")
}

description = "simple-lock-component"

dependencies {
    api(project(":backend:components:lock-component:api-lock-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-logging")

}