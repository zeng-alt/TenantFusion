description = "jetcache-storage-component"

dependencies {
    implementation(project(":backend:components:storage-component:api-storage-component"))
    implementation(project(":backend:components:lock-component:api-lock-component"))
    implementation(rootProject.libs.jetcache.starter.redis)
    implementation("com.github.ben-manes.caffeine:caffeine")
}
