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

subprojects {
    // 仅处理 backend 子模块
    if (!path.startsWith(":backend")) return@subprojects

    apply(plugin = "io.spring.dependency-management")

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom(rootProject.libs.spring.modulith.bom.get().toString())
        }
    }
}
