plugins {
    id("java-library")
}

description = "配置服务的 JDBC 存储适配"

dependencies {
    api(project(":backend:components:config-component:server-config-component"))
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":backend:components:config-component:client-config-component"))
    testRuntimeOnly("com.h2database:h2")
}
