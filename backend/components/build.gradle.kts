description = "components"
group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

// 禁用父模块的打包任务
tasks.withType<Jar> {
    enabled = false
}
tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    enabled = false
    mainClass.set("none")
}

subprojects {
    apply(plugin = "java")

    // 组件模块通常作为依赖库，不需要打成可执行的 Spring Boot Jar
    tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
        enabled = false
        mainClass.set("none")
    }
    // 开启普通 Jar 打包，以便其他模块引用
    tasks.withType<Jar> {
        enabled = true
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        "testImplementation"(platform(libs.junit.platform))
        "testImplementation"(libs.junit.jupiter)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}