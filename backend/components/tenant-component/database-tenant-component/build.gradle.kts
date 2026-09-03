plugins {
    id("java-library")
}

description = "database-tenant-component"

dependencies {
    api(project(":backend:components:tenant-component:core-tenant-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-jdbc")
    compileOnly("com.zaxxer:HikariCP")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.zaxxer:HikariCP")
    testImplementation(rootProject.libs.h2)
}
