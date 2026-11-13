plugins {
    id("java-library")
}

description = "rabbit-event-component"

dependencies {
    api(project(":backend:components:event-component:api-event-component"))
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    api(rootProject.libs.spring.modulith.events.amqp)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
