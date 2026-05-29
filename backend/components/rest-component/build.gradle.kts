plugins {
    id("java-library")
}

description = "REST 注解与 APT 代码生成"

tasks.withType<Jar> {
    enabled = false
}
