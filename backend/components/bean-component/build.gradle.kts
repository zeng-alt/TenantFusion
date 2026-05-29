plugins {
    id("java-library")
}

description = "spring bean工具包"

dependencies {
    api(project(":backend:components:api-component"))
    api(rootProject.libs.vavr)
    implementation("org.springframework:spring-context")
}
