description = "event-components"

tasks.withType<Jar> {
    enabled = false
}

subprojects {
    pluginManager.withPlugin("java") {
        apply(plugin = "jacoco")
        extensions.configure<JacocoPluginExtension> {
            toolVersion = "0.8.14"
        }
        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("jacocoTestReport"))
            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        minimum = "0.80".toBigDecimal()
                    }
                }
            }
        }
        tasks.named("check") {
            dependsOn(tasks.named("jacocoTestCoverageVerification"))
        }
    }
}
