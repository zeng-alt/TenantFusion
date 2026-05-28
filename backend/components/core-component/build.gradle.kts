plugins {
    id("java")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":backend:components:api-component"))
    api(project(":backend:components:json-component"))
    api(project(":backend:components:bean-component"))
    api("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("jakarta.servlet:jakarta.servlet-api")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
}

tasks.test {
    useJUnitPlatform()
}