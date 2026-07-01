plugins {
    id("java-library")
}

description = "redis-message-component"

dependencies {
    api(project(":backend:components:message-component:api-message-component"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation(rootProject.libs.redisson.spring.boot.starter)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
