plugins {
    id("java-library")
    jacoco
}

description = "ABAC 对外契约与属性模型"

dependencies {
    api(rootProject.libs.vavr)
    implementation("org.springframework:spring-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
