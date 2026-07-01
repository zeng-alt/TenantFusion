plugins {
    id("java-library")
}

description = "redis-message-component"

dependencies {
    api(project(":backend:components:message-component:api-message-component"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation(rootProject.libs.redisson.spring.boot.starter)
    implementation(rootProject.libs.spring.modulith.starter.core)
    implementation("org.springframework.modulith:spring-modulith-events-api")
    implementation("org.springframework.modulith:spring-modulith-events-core")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
