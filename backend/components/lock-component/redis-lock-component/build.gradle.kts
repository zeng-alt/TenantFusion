plugins {
    id("java-library")
}

description = "redis-lock-component"

dependencies {
    api(project(":backend:components:lock-component:api-lock-component"))
    implementation(rootProject.libs.redisson.spring.boot.starter)

}