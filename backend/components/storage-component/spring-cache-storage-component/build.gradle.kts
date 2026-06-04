description = "spring-cache-storage-component"

dependencies {
    implementation(project(":backend:components:storage-component:api-storage-component"))
    implementation(rootProject.libs.spring.boot.starter.cache)
}
