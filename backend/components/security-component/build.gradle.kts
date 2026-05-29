plugins {
    id("java-library")
}

description = "安全认证与授权组件"

dependencies {
    api(project(":backend:components:core-component"))
    api(project(":backend:components:domain-component"))
    api("org.springframework.boot:spring-boot-starter-security")
}
