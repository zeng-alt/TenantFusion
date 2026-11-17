plugins {
    id("java-library")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"


dependencies {
    api(project(":backend:components:log-component:api-log-component"))
    api(project(":backend:components:security-component:api-security-component"))
    api(project(":backend:components:json-component"))
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("org.springframework:spring-aop")
    implementation(rootProject.libs.vavr)
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}

