plugins {
    id("java-library")
}

description = "验证码组件（支持图片/算术/随机码三种模式）"

dependencies {
    implementation(project(":backend:components:storage-component:api-storage-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
