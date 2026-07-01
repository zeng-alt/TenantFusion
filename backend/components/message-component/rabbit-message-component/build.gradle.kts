plugins {
    id("java-library")
}

description = "rabbit-message-component"

dependencies {
    api(project(":backend:components:message-component:api-message-component"))
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    api(rootProject.libs.spring.modulith.events.amqp)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
