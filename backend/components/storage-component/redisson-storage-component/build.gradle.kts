description = "redisson-storage-component"

dependencies {
    implementation(project(":backend:components:storage-component:api-storage-component"))
    api(project(":backend:components:json-component"))
    implementation(rootProject.libs.redisson.spring.boot.starter)
}
