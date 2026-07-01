plugins {
    id("java-library")
}

description = "message-log-component"

dependencies {
    api(project(":backend:components:log-component:api-log-component"))
    implementation(project(":backend:components:log-component:core-log-component"))
    api(project(":backend:components:message-component:api-message-component"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-tx")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
