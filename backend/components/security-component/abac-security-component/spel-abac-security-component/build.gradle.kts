plugins {
    id("java-library")
    jacoco
}

description = "ABAC 受限 SpEL 求值实现与技术自动配置"

dependencies {
    api(project(":backend:components:security-component:abac-security-component:core-abac-security-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-expression")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
