plugins {
    id("java-library")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

java {
    // Spring MVC 需要 -parameters 来解析 @PathVariable 参数名
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

dependencies {
    api(project(":backend:components:oss-component:core-oss-component"))
    api(project(":backend:components:domain-component"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // Web（用于内置的 CRUD REST 控制器）
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    // @CrudRest 注解（SOURCE 保留，仅编译时需要）
    compileOnly(project(":backend:components:rest-component:rest-annotation-component"))

    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(rootProject.libs.h2)
    testRuntimeOnly("jakarta.servlet:jakarta.servlet-api")
}

