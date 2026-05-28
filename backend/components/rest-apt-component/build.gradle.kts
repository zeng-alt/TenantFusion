plugins {
    id("java-library")
}

group = "com.github.zeng.alt"

dependencies {
    // JavaPoet — 编译期源码生成
    implementation("com.squareup:javapoet:1.13.0")

    // AutoService — 自动注册 Processor
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    // 依赖 domain-component（获取 BaseRepository、RestResponse 等类型）
    api(project(":backend:components:domain-component"))

    // Spring 类型依赖
    api("org.springframework:spring-webmvc")
    api("org.springframework.data:spring-data-commons")
    api("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("jakarta.annotation:jakarta.annotation-api")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
