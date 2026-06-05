plugins {
    id("java-library")
}

description = "api-lock-component"

dependencies {
    implementation(project(":backend:components:api-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")

}
