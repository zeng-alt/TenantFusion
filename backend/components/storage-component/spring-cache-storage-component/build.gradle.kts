description = "spring-cache-storage-component"

dependencies {
    implementation(project(":backend:components:storage-component:api-storage-component"))
    implementation(project(":backend:components:lock-component:api-lock-component"))
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(rootProject.libs.spring.boot.starter.cache)
}
