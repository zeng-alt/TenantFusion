plugins {
    id("java-library")
}

description = "databases-lock-component"

dependencies {
    api(project(":backend:components:lock-component:api-lock-component"))
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly(rootProject.libs.h2)


}