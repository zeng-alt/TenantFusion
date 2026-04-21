description = "admin-application"
group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

plugins {
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.hibernate.orm)
}


graalvmNative {
    binaries {
        named("main") {
            buildArgs.addAll(
                "-H:-CheckToolchain",
                "--no-fallback",
                "--install-exit-handlers",
                "--enable-url-protocols=http,https",

                // 字符编码相关
                "-Dfile.encoding=UTF-8",                 // 设置文件编码
                "-Duser.country=CN",                     // 设置国家
                "-Duser.language=zh"                     // 设置语言
            )
        }
    }
    metadataRepository {
        enabled.set(true)
        // 可以指定 metadata 仓库版本
        version.set("0.3.15") // 或更高
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    outputs.upToDateWhen { false }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    // 本地开发运行（bootRun）时默认使用 dev 配置文件
    systemProperty("spring.profiles.active", System.getProperty("spring.profiles.active", "dev"))
}

// 接收外部传入的 profiles.active 参数，默认为 prod
val activeProfile = project.findProperty("profiles.active") as? String ?: "dev"

dependencies {
    // 可以在这里引入 admin 模块特有的依赖，例如 Web, JPA 等
    if (activeProfile != "prod") {
        implementation(libs.liquibase.core)
    }
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.h2)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.starter.jpa)
    compileOnly(libs.lombok)
    developmentOnly(libs.spring.boot.docker.compose)
    implementation(libs.spring.boot.starter.data.redis)
    annotationProcessor(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.spring.security.test)
    testCompileOnly(libs.lombok)
    testRuntimeOnly(libs.junit.platform.launcher)
    testAnnotationProcessor(libs.lombok)
}
