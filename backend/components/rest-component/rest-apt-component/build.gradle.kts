plugins {
    id("java-library")
}

description = "REST 注解 APT 处理器"

dependencies {
    api(project(":backend:components:rest-component:rest-annotation-component"))
    api(project(":backend:components:api-component"))
    api("com.squareup:javapoet:1.13.0")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    implementation("org.springframework:spring-webmvc")
    implementation(rootProject.libs.vavr)
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework:spring-context")
    implementation("jakarta.servlet:jakarta.servlet-api")
}
