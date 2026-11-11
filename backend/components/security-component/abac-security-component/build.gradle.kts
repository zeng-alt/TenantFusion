import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    base
}

description = "ABAC 子模块聚合：API、核心授权引擎与 SpEL 实现"

subprojects {
    plugins.withId("jacoco") {
        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("test"))
            violationRules {
                rule { limit { minimum = "0.80".toBigDecimal() } }
            }
        }
        tasks.named("check") {
            dependsOn("jacocoTestCoverageVerification")
        }
    }
}

// components 父级统一应用 java-library；此处只保留其测试聚合入口，不发布空 Jar。
tasks.withType<Jar>().configureEach {
    enabled = false
}

tasks.named("test") {
    dependsOn(subprojects.map { "${it.path}:test" })
}

listOf("jacocoTestReport", "jacocoTestCoverageVerification").forEach { taskName ->
    tasks.register(taskName) {
        dependsOn(subprojects.map { "${it.path}:$taskName" })
    }
}

tasks.named("check") {
    dependsOn(subprojects.map { "${it.path}:check" })
}

tasks.named("assemble") {
    dependsOn(subprojects.map { "${it.path}:assemble" })
}
