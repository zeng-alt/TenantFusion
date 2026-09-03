plugins {
    id("java-library")
}

description = "table-tenant-component"

dependencies {
    api(project(":backend:components:tenant-component:core-tenant-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("jakarta.persistence:jakarta.persistence-api")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(rootProject.libs.h2)
}
