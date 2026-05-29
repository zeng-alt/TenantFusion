plugins {
    id("java-library")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

dependencies {
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)
}

tasks.test {
    useJUnitPlatform()
}