description = "spring-cache-storage-component"

dependencies {
    api(project(":backend:components:storage-component:api-storage-component"))
    api(project(":backend:components:lock-component:api-lock-component"))
    api("com.github.ben-manes.caffeine:caffeine")
    api(rootProject.libs.spring.boot.starter.cache)
}
