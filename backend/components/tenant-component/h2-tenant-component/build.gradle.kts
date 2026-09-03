plugins {
    id("java-library")
}

description = "h2-tenant-component"

dependencies {
    api(project(":backend:components:tenant-component:api-tenant-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
