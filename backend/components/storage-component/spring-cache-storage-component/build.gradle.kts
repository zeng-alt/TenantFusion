description = "spring-cache-storage-component"

dependencies {
    implementation("org.springframework.boot:spring-boot-cache")
    api(project(":backend:components:storage-component:api-storage-component"))
    api(project(":backend:components:lock-component:api-lock-component"))
    api("com.github.ben-manes.caffeine:caffeine")
    api(rootProject.libs.spring.boot.starter.cache)
}
