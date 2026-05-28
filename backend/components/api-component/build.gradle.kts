plugins {
    id("java-library")
}

group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(rootProject.libs.lombok)
    api("org.springframework.boot:spring-boot-autoconfigure")
    api(rootProject.libs.swagger.annotations.jakarta)
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
}

tasks.test {
    useJUnitPlatform()
}