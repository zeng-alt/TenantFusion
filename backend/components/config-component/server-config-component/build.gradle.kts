plugins {
    id("java-library")
}
group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

dependencies {
    api(project(":backend:components:config-component:api-config-component"))
    api(project(":backend:components:domain-component"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-webmvc")
    implementation("org.slf4j:slf4j-api")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }
}
