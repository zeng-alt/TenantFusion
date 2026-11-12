description = "redisson-storage-component"

dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-jackson2")
    api(project(":backend:components:storage-component:api-storage-component"))
    api(project(":backend:components:json-component"))
    api(rootProject.libs.redisson.spring.boot.starter)
    implementation("org.springframework:spring-web")
}
