plugins {
    id("java-library")
}

description = "remote-engine-component"

dependencies {
    api(project(":backend:components:camunda-component:api-engine-component"))

    implementation("io.holunda.c7:c7-rest-client-spring-boot-starter-feign:2026.04.2")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
