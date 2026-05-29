description = "components"

tasks.withType<Jar> {
    enabled = false
}

subprojects {
    apply(plugin = "java-library")

    // 组件子模块都不是可执行的 Spring Boot 应用，禁用 bootJar
    plugins.withId("org.springframework.boot") {
        tasks.matching { it.name == "bootJar" }.configureEach {
            enabled = false
        }
    }

    // 统一 Lombok + 测试依赖配置
    dependencies {
        add("compileOnly", rootProject.libs.lombok)
        add("annotationProcessor", rootProject.libs.lombok)
        add("testCompileOnly", rootProject.libs.lombok)
        add("testAnnotationProcessor", rootProject.libs.lombok)
    }

//    tasks.withType<Test> {
//        useJUnitPlatform()
//    }
}
