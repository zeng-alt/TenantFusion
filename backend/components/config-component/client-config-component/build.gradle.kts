plugins {
    id("java-library")
}
group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    api(project(":backend:components:config-component:api-config-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("tools.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
    implementation(rootProject.libs.spring.cloud.context)
}
