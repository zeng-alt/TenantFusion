plugins {
    id("java-library")
}
group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

dependencies {
    api(project(":backend:components:config-component:api-config-component"))
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework:spring-tx")
    implementation(project(":backend:components:json-component"))
    implementation("org.slf4j:slf4j-api")
    implementation(rootProject.libs.mapstruct)
    annotationProcessor(rootProject.libs.lombok.mapstruct.binding)
    annotationProcessor(rootProject.libs.mapstruct.processor)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":backend:components:config-component:client-config-component"))
    testImplementation("jakarta.servlet:jakarta.servlet-api")
}
