plugins {
    id("java-library")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

dependencies {
    api(project(":backend:components:oss-component:core-oss-component"))
    api(project(":backend:components:api-component"))
    // Spring JDBC
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    // Spring Boot 自动配置
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // Web（用于内置的 CRUD REST 控制器）
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
