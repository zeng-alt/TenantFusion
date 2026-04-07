plugins {
    id("org.springframework.boot") version "3.5.13" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.hibernate.orm") version "6.6.45.Final" apply false
    id("org.graalvm.buildtools.native") version "0.10.6" apply false
}

allprojects {
    group = "com.github.zeng.alt"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}


// 统一配置所有后端相关的模块（排除 frontend 模块）
configure(subprojects) {
    apply(plugin = "io.spring.dependency-management")


    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom("org.springframework.modulith:spring-modulith-bom:1.4.10")
        }

        dependencies {
            // 统一管理第三方库版本
            dependency("com.google.guava:guava:32.1.3-jre")
            dependency("org.apache.commons:commons-lang3:3.14.0")
            dependency("com.fasterxml.jackson.core:jackson-databind:2.17.0")
            dependency("io.jsonwebtoken:jjwt-api:0.12.5")
            dependency("io.jsonwebtoken:jjwt-impl:0.12.5")
            dependency("io.jsonwebtoken:jjwt-jackson:0.12.5")
        }
    }

    dependencies {
        // 可以在此处统一管理所有子模块都需要的依赖
        // implementation(platform("com.fasterxml.jackson:jackson-bom:2.17.0"))
    }
}

// 前端任务（与之前相同）
val frontendDir = file("${rootDir}/frontend")
val frontendDistDir = file("${frontendDir}/dist")
val staticDir = file("${rootDir}/backend/admin/src/main/resources/static")

tasks.register<Delete>("cleanFrontend") {
    group = "build"
    description = "Clean Vue frontend static resources"
    delete(staticDir)
}

tasks.register<Exec>("buildFrontend") {
    dependsOn("cleanFrontend")
    workingDir(frontendDir)
    group = "build"
    description = "Build Vue frontend"
    val npmCmd = if (System.getProperty("os.name").lowercase().contains("windows")) "pnpm.cmd" else "pnpm"
    commandLine(npmCmd, "run", "build")
    inputs.dir(file("${frontendDir}/src"))
    inputs.file(file("${frontendDir}/package.json"))
    inputs.file(file("${frontendDir}/package-lock.json"))
    outputs.dir(frontendDistDir)
}

tasks.register<Copy>("copyFrontend") {
    group = "build"
    description = "Copy Vue frontend to backend static resources"
    dependsOn("buildFrontend")
    from(frontendDistDir)
    into(staticDir)
}

project(":backend:admin") {
    plugins.withId("java") {
        tasks.named("compileJava") {
            dependsOn(rootProject.tasks.named("copyFrontend"))
        }
        tasks.named("processResources") {
            dependsOn(rootProject.tasks.named("copyFrontend"))
        }
    }
}