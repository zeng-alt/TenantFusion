plugins {
    id("java-library")
}

description = "simple-lock-component"

dependencies {
    implementation(project(":backend:components:lock-component:api-lock-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")

}