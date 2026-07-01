plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.hibernate.orm) apply false
    alias(libs.plugins.graalvm.native) apply false
//    `maven-publish` apply false
}



allprojects {
    group = "com.github.zeng.alt"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    // 仅处理 backend 子模块
    if (!path.startsWith(":backend")) return@subprojects

    apply(plugin = "io.spring.dependency-management")

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springdoc:springdoc-openapi-bom:2.8.17")
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom(rootProject.libs.spring.modulith.bom.get().toString())
            mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.28.1")
            mavenBom(rootProject.libs.aws.bom.get().toString())
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    plugins.withType<JavaPlugin>().configureEach {
        dependencies {
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }
    }

}