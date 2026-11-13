plugins {
    id("java-library")
}

description = "redis-event-component"

dependencies {
    api("org.springframework.data:spring-data-redis")
    implementation(project(":backend:components:json-component"))
    api(project(":backend:components:event-component:api-event-component"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation(rootProject.libs.redisson.spring.boot.starter)
    implementation(rootProject.libs.rxjava3)
    implementation(rootProject.libs.spring.modulith.starter.core)
    implementation("org.springframework.modulith:spring-modulith-events-api")
    implementation("org.springframework.modulith:spring-modulith-events-core")

    testImplementation(testFixtures(project(":backend:components:event-component:api-event-component")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers")
}
