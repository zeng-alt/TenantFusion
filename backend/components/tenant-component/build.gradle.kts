plugins {
    id("java-library")
}

description = "多租户组件"

tasks.withType<Jar> {
    enabled = false
}
