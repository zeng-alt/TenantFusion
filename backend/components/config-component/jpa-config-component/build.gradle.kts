plugins {
    id("java-library")
}

description = "配置服务的默认 JPA 存储适配"

dependencies {
    api(project(":backend:components:config-component:server-config-component"))
    api(project(":backend:components:domain-component"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation(rootProject.libs.mapstruct)
    annotationProcessor(rootProject.libs.lombok.mapstruct.binding)
    annotationProcessor(rootProject.libs.mapstruct.processor)
    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact { classifier = "jakarta" }
    }
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":backend:components:config-component:client-config-component"))
    testImplementation(project(":backend:components:config-component:jdbc-config-component"))
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testRuntimeOnly("com.h2database:h2")
}
