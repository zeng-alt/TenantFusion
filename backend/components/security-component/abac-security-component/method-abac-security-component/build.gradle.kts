plugins {
    id("java-library")
    jacoco
}

description = "ABAC 方法级注解授权：拦截器、注册表缓存与宿主 SPI"

dependencies {
    api(project(":backend:components:security-component:abac-security-component:api-abac-security-component"))
    api(project(":backend:components:security-component:abac-security-component:core-abac-security-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-aop")
    implementation("org.springframework.security:spring-security-core")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}