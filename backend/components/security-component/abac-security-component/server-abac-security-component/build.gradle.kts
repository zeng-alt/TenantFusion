plugins {
    id("java-library")
    jacoco
}

description = "ABAC 服务端：规则管理、权威查询和变更分发"

dependencies {
    api(project(":backend:components:security-component:abac-security-component:core-abac-security-component"))
    implementation(project(":backend:components:security-component:core-security-component"))
    implementation(project(":backend:components:event-component:api-event-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework:spring-tx")
    implementation("org.slf4j:slf4j-api")
    implementation(rootProject.libs.guava)
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(project(":backend:components:security-component:abac-security-component:client-abac-security-component"))
    testImplementation(project(":backend:components:security-component:abac-security-component:method-abac-security-component"))
}
