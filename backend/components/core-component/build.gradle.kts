plugins {
    id("java-library")
}

dependencies {
    api(project(":backend:components:api-component"))
    api(project(":backend:components:json-component"))
    api(project(":backend:components:bean-component"))
    api("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("jakarta.servlet:jakarta.servlet-api")

    api("org.apache.commons:commons-lang3")
    api("commons-io:commons-io")
    api("commons-codec:commons-codec")
}
