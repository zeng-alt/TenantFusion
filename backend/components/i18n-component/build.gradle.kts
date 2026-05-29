plugins {
    id("java-library")
}

description = "国际化组件（支持文件/数据库两种模式）"

dependencies {
    api(project(":backend:components:api-component"))
    api(project(":backend:components:domain-component"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(rootProject.libs.h2)
}
