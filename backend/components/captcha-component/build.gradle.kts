plugins {
    id("java-library")
}

description = "验证码组件（支持算术/随机码两种模式，均生成图片）"

dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    implementation(project(":backend:components:storage-component:api-storage-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
