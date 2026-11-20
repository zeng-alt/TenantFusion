plugins {
    id("java-library")
}

description = "embedded-engine-component"

dependencies {
    api(project(":backend:components:camunda-component:api-engine-component"))
    implementation("org.springframework:spring-context")

    // Operaton 嵌入式引擎及 REST、Webapp、Connect、Spin 集成。
    implementation("org.operaton.bpm.springboot:operaton-bpm-spring-boot-starter:${rootProject.libs.versions.operaton.get()}")
    implementation("org.operaton.bpm.springboot:operaton-bpm-spring-boot-starter-webapp:${rootProject.libs.versions.operaton.get()}")
    implementation("org.operaton.bpm.springboot:operaton-bpm-spring-boot-starter-rest:${rootProject.libs.versions.operaton.get()}")
    implementation("org.operaton.bpm:operaton-engine-plugin-connect:${rootProject.libs.versions.operaton.get()}")
    implementation("org.operaton.connect:operaton-connect-http-client:${rootProject.libs.versions.operaton.get()}")
    implementation("org.operaton.bpm:operaton-engine-plugin-spin:${rootProject.libs.versions.operaton.get()}")
    implementation("org.operaton.spin:operaton-spin-dataformat-json-jackson:${rootProject.libs.versions.operaton.get()}")
    implementation("org.graalvm.polyglot:polyglot:24.2.1")
    implementation("org.graalvm.polyglot:js:24.2.1")
    implementation("org.graalvm.js:js-scriptengine:24.2.1")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":backend:components:camunda-component:api-camunda-component"))
    testImplementation(project(":backend:components:security-component:core-security-component"))
    testRuntimeOnly(rootProject.libs.h2)
}
