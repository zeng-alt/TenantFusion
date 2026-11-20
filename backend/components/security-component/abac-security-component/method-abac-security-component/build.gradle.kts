plugins {
    id("java-library")
    jacoco
}

description = "ABAC 方法授权与受限 SpEL：拦截器、预解析和表达式缓存"

dependencies {
    api(project(":backend:components:security-component:abac-security-component:api-abac-security-component"))
    api(project(":backend:components:security-component:abac-security-component:core-abac-security-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-aop")
    implementation("org.springframework:spring-expression")
    implementation("org.springframework:spring-tx")
    implementation(rootProject.libs.guava)
    implementation("org.springframework.security:spring-security-core")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework:spring-jdbc")
    testRuntimeOnly("com.h2database:h2")
    testImplementation(project(":backend:components:security-component:abac-security-component:client-abac-security-component"))
}
