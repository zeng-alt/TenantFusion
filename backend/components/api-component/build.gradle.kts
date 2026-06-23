plugins {
    id("java-library")
}

dependencies {
    // 对外暴露的 API
    api("jakarta.validation:jakarta.validation-api")
    api(rootProject.libs.swagger.annotations.jakarta)
    api(rootProject.libs.vavr)
    api(rootProject.libs.guava)

    // 内部实现依赖
//    implementation("org.apache.commons:commons-lang3")
//    implementation("commons-io:commons-io")
//    implementation("commons-codec:commons-codec")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
}
