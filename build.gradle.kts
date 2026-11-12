plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.hibernate.orm) apply false
    alias(libs.plugins.graalvm.native) apply false
//    `maven-publish` apply false
}


// 覆盖 Boot 3.5.16 尚未包含的同主版本安全修复；应用和组件使用同一版本基线。
val securityDependencyVersions = mapOf(
    "commons-lang3.version" to "3.18.0",
    "httpclient5.version" to "5.6.3",
    "httpcore5.version" to "5.4.3",
    "jackson-bom.version" to "2.21.7",
    "log4j2.version" to "2.25.5",
    "netty.version" to "4.1.137.Final",
    "postgresql.version" to "42.7.12",
    "tomcat.version" to "10.1.59"
)



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

    securityDependencyVersions.forEach { (name, version) -> extra[name] = version }
    apply(plugin = "io.spring.dependency-management")

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        dependencies {
            dependency("org.bouncycastle:bcprov-jdk18on:1.85")
        }
        imports {
            mavenBom("org.springdoc:springdoc-openapi-bom:2.8.17")
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES) {
                securityDependencyVersions.forEach { (name, version) -> bomProperty(name, version) }
            }
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
        // 保留方法参数名：Spring 6.2 的 DefaultParameterNameDiscoverer 已移除 LVT 回退，
        // ABAC 方法拦截器按参数名（#id 等）注入上下文必须依赖 -parameters 编译标志。
        tasks.withType<JavaCompile>().configureEach {
            options.compilerArgs.add("-parameters")
        }
        dependencies {
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        }
    }

}