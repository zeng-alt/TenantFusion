plugins {
    id("java-library")
}

description = "databases-lock-component"

dependencies {
    api(project(":backend:components:lock-component:api-lock-component"))
    api(project(":backend:components:domain-component"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly(rootProject.libs.h2)
}