plugins {
    id("java-library")
}

description = "kafka-message-component"

dependencies {
    api(project(":backend:components:message-component:api-message-component"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
