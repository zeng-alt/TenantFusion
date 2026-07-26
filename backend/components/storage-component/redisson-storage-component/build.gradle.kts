description = "redisson-storage-component"

dependencies {
    api(project(":backend:components:storage-component:api-storage-component"))
    api(project(":backend:components:json-component"))
    api(rootProject.libs.redisson.spring.boot.starter)
    implementation("org.springframework:spring-web")
}
