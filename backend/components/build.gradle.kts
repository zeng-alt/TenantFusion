plugins {
    `maven-publish`
    id("org.springframework.boot")
}

description = "components"

tasks.withType<Jar> {
    enabled = false
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

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

    // 统一发布配置：只对含有源码的叶子模块（有 src/main 目录）发布到 Nexus
    // 容器模块（lock-component、storage-component 等）仅有 build.gradle.kts 和子模块，没有 src/main，会被自动跳过
    if (project.projectDir.resolve("src/main").isDirectory) {
        publishing {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    artifactId = project.name
                }
            }
            repositories {
                maven {
                    url = uri("http://192.168.56.106:8081/repository/maven-snapshots")
                    isAllowInsecureProtocol = true
                    credentials {
                        username = "admin"
                        password = "38b802a4-df6e-4931-a4a1-c59dbef089df"
                    }
                }
            }
        }
    }
}
