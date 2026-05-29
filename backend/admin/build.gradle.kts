description = "admin-application"

plugins {
    id("java")
    id("org.springframework.boot")
    id("org.hibernate.orm")
    id("org.graalvm.buildtools.native")
}

graalvmNative {
    binaries {
        named("main") {
            buildArgs.addAll(
                "-H:-CheckToolchain",
                "--no-fallback",
                "--install-exit-handlers",
                "--enable-url-protocols=http,https",
                "-Dfile.encoding=UTF-8",
                "-Duser.country=CN",
                "-Duser.language=zh"
            )
        }
    }
    metadataRepository {
        enabled.set(true)
        version.set("0.3.15")
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    outputs.upToDateWhen { false }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("spring.profiles.active", System.getProperty("spring.profiles.active", "dev"))
}

val activeProfile = project.findProperty("profiles.active") as? String ?: "dev"

dependencies {
    // 仅在非生产环境引入 Liquibase
    if (activeProfile != "prod") {
        implementation(libs.liquibase.core)
    }

    // 运行时数据库驱动
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.h2)

    // 组件模块依赖
    implementation(project(":backend:components:rest-component:rest-annotation-component"))
    implementation(project(":backend:components:core-component"))
    implementation(project(":backend:components:domain-component"))
    implementation(project(":backend:components:i18n-component"))
    annotationProcessor(project(":backend:components:rest-component:rest-apt-component"))

    // Spring Boot Starters（从组件模块继承，显式声明确保 IDE 感知）
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.starter.jpa)
    implementation(libs.spring.boot.starter.data.redis)

    // 编译时工具
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Docker Compose 开发支持
    developmentOnly(libs.spring.boot.docker.compose)

    // 测试
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.spring.security.test)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testRuntimeOnly(libs.junit.platform.launcher)
}
