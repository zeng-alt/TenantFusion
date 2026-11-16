description = "config-component"

subprojects {
    if (name == "api-config-component") return@subprojects
    pluginManager.withPlugin("java") {
        apply(plugin = "jacoco")
        extensions.configure<JacocoPluginExtension> {
            toolVersion = "0.8.14"
        }
        val businessClasses = the<SourceSetContainer>()["main"].output.asFileTree.matching {
            exclude("**/entity/**", "**/model/**", "**/mapper/**", "**/aot/**")
        }
        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            classDirectories.setFrom(businessClasses)
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
        }
        tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("jacocoTestReport"))
            classDirectories.setFrom(businessClasses)
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
