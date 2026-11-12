plugins {
    id("java-library")
}

description = "api-camunda-component"

dependencies {
    api("org.operaton.bpm:operaton-engine:${rootProject.libs.versions.operaton.get()}")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation(project(":backend:components:security-component:core-security-component"))
    implementation("org.springframework:spring-web")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
