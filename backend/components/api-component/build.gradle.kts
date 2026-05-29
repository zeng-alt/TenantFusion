plugins {
    id("java-library")
}

dependencies {
    // 对外暴露的 API
    api("jakarta.validation:jakarta.validation-api")
    api(rootProject.libs.swagger.annotations.jakarta)

    // 内部实现依赖
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
}
