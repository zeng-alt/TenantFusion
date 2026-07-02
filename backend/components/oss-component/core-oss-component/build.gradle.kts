plugins {
    id("java-library")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"


dependencies {
    api(project(":backend:components:oss-component:api-oss-component"))
    // AWS SDK for Java 2.x - S3
    api(platform(rootProject.libs.aws.bom))
    implementation("software.amazon.awssdk:s3") {
        exclude(group = "software.amazon.awssdk", module = "aws-crt-client")
        exclude(group = "software.amazon.awssdk", module = "apache-client")
        exclude(group = "software.amazon.awssdk", module = "url-connection-client")
    }
    // 适用于 Netty 的异步 HTTP 客户端
    implementation("software.amazon.awssdk:netty-nio-client")
    // Spring Boot 自动配置
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // 日志门面（SLF4J）
    implementation("org.slf4j:slf4j-api")
    // Spring Web（用于可选的 OSS 管理端点）
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
