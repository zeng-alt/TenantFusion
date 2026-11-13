plugins {
    id("java-library")
}

description = "rabbit-event-component"

dependencies {
    api("org.springframework.amqp:spring-rabbit")
    api(project(":backend:components:event-component:api-event-component"))
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    api(rootProject.libs.spring.modulith.events.amqp)

    testImplementation(testFixtures(project(":backend:components:event-component:api-event-component")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers")
}
