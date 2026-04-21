description = "components"

tasks.withType<Jar> {
    enabled = false
}

subprojects {
    apply(plugin = "java-library")

    plugins.withId("org.springframework.boot") {
        tasks.matching { it.name == "bootJar" }.configureEach {
            enabled = false
        }
    }

    dependencies {
        testImplementation(platform(rootProject.libs.junit.bom))
        testImplementation(rootProject.libs.junit.jupiter)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
