description = "admin-application"

plugins {
    id("java")
    id("org.springframework.boot")
    id("org.hibernate.orm")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    outputs.upToDateWhen { false }
}

dependencies {
    // Web管理平台
    implementation(project(":backend:components:security-component:jwt-auth-security-component"))
    implementation(project(":backend:components:storage-component:redisson-storage-component"))
    implementation(project(":backend:module:workflow-module"))
    implementation(project(":backend:components:camunda-component:remote-camunda-component"))
    // 多租户远程源：经 OpenFeign 读 admin 的租户元数据端点（无租户库直连权限）
    implementation(project(":backend:components:tenant-component:remote-tenant-component"))
    // 多租户混合模式：逐租户路由（hybrid）+ 四档策略 + 方言，与 admin 对齐
    implementation(project(":backend:components:tenant-component:row-tenant-component"))
    implementation(project(":backend:components:tenant-component:hybrid-tenant-component"))
    implementation(project(":backend:components:tenant-component:schema-tenant-component"))
    implementation(project(":backend:components:tenant-component:database-tenant-component"))
    implementation(project(":backend:components:tenant-component:table-tenant-component"))
    implementation(project(":backend:components:tenant-component:h2-tenant-component"))
    implementation(project(":backend:components:tenant-component:pg-tenant-component"))
    // 嵌入式引擎实现（远程部署时替换为 remote-engine-component）
    implementation(project(":backend:components:camunda-component:embedded-engine-component"))
    // 引擎 API（编译 formkService 等 delegate 表达式 Bean 需要，运行时由 embedded-engine-component 提供）
    implementation("org.operaton.bpm:operaton-engine:${rootProject.libs.versions.operaton.get()}")
    // Camunda REST API 的 OpenAPI 定义
//    implementation("org.operaton.bpm:operaton-engine-rest-openapi:${rootProject.libs.versions.operaton.get()}")
    runtimeOnly(libs.h2)
    implementation("org.springframework.boot:spring-boot-starter-web")
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//    implementation(libs.spring.boot.starter.validation)
//    implementation("org.operaton.bpm.springboot:operaton-bpm-spring-boot-starter-webapp:${rootProject.libs.versions.operaton.get()}")
//    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
//    implementation("org.springframework.boot:spring-boot-starter-actuator")
//
    compileOnly(libs.lombok)
//
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}
