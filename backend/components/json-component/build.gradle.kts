plugins {
    id("java-library")
}

dependencies {
    api(project(":backend:components:api-component"))
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    api(rootProject.libs.vavr.jackson)

    // Spring 框架依赖（JacksonHelper 使用 spring-core, JsonConfiguration 使用 autoconfigure/context）
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-beans")
}
