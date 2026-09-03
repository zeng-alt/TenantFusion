plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.hibernate.orm) apply false
    alias(libs.plugins.graalvm.native) apply false
//    `maven-publish` apply false
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
        // 显式声明 JDK 21：代码用到了 21 的 API（如 SequencedCollection.getLast），
        // 不声明的话会退化成"用环境里的任意 JDK 编译"，在 JDK 17 上报的是找不到符号这类误导性错误。
        // settings.gradle.kts 里的 foojay-resolver 会在本机缺少 21 时自动下载。
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
        dependencies {
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }
    }

}