plugins {
    id("java-library")
}

description = "kafka-event-component"

dependencies {
    api(project(":backend:components:event-component:api-event-component"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.kafka:spring-kafka")
    api(rootProject.libs.spring.modulith.events.kafka)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
}
