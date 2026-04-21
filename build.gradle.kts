plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.hibernate.orm) apply false
    alias(libs.plugins.graalvm.native) apply false
}

allprojects {
    group = "com.github.zeng.alt"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

val myLibs = libs

subprojects {
    if (!path.startsWith(":backend")) return@subprojects

    apply(plugin = "io.spring.dependency-management")

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom(myLibs.spring.modulith.bom.get().toString())
        }

        dependencies {
            // 统一管理第三方库版本

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
    inputs.file(file("${frontendDir}/pnpm-lock.yaml"))
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
