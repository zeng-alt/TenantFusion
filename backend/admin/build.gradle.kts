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
                "-H:+ReportExceptionStackTraces", // 打印完整异常堆栈
                "-H:-CheckToolchain",
                "--no-fallback",
                "--report-unsupported-elements-at-runtime",
                "--install-exit-handlers",
                "--enable-url-protocols=http,https",
                "-Dfile.encoding=UTF-8",
                "-Duser.country=CN",
                "-Duser.language=zh",
                "-H:+AddAllCharsets",
                "-H:ClassInitialization=org.apache.commons.logging.LogFactory:run_time"
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
    if (activeProfile != "prod") {
        implementation(libs.liquibase.core)
    }
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.h2)
    implementation(project(":backend:components:rest-component:rest-annotation-component"))
    implementation(project(":backend:components:core-component"))
    implementation(project(":backend:components:domain-component"))
    implementation(project(":backend:components:i18n-component"))
    annotationProcessor(project(":backend:components:rest-component:rest-apt-component"))
    implementation(project(":backend:components:storage-component:api-storage-component"))
    implementation(project(":backend:components:storage-component:spring-cache-storage-component"))
    implementation(project(":backend:components:lock-component:api-lock-component"))
    implementation(project(":backend:components:lock-component:simple-lock-component"))
    implementation(project(":backend:components:security-component:jwt-auth-security-component"))
    implementation(project(":backend:components:security-component:cookie-auth-security-component"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.starter.jpa)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.data.redis)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.spring.boot.configuration.processor)
    developmentOnly(libs.spring.boot.docker.compose)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.spring.security.test)
}
