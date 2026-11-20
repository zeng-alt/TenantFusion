plugins {
    id("java-library")
    jacoco
}

description = "ABAC 客户端：策略缓存、远程查询、预解析和变更刷新"

dependencies {
    api(project(":backend:components:security-component:abac-security-component:core-abac-security-component"))
    implementation(project(":backend:components:event-component:api-event-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-http-converter")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework:spring-tx")
    implementation("org.slf4j:slf4j-api")
    implementation(rootProject.libs.guava)
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation(project(":backend:components:security-component:abac-security-component:method-abac-security-component"))
}
