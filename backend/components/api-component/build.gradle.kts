plugins {
    id("java-library")
}

description = "组件基础 API：枚举、响应体、异常、分页查询等"

dependencies {
    // 对外暴露的 API
    api("jakarta.validation:jakarta.validation-api")
    api(rootProject.libs.swagger.annotations.jakarta)
    api(rootProject.libs.vavr)
    api(rootProject.libs.guava)

    // 内部实现依赖
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
}
