plugins {
    id("java-library")
}

description = "jpa-log-component"

dependencies {
    implementation("org.springframework.boot:spring-boot-hibernate")
    implementation(rootProject.libs.vavr)
    implementation(rootProject.libs.mapstruct)
    annotationProcessor(rootProject.libs.lombok.mapstruct.binding)
    annotationProcessor(rootProject.libs.mapstruct.processor)
    api(project(":backend:components:log-component:api-log-component"))
    api(project(":backend:components:domain-component"))
    implementation(project(":backend:components:log-component:core-log-component"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-jackson")
    testImplementation(project(":backend:components:log-component:core-log-component"))
    testImplementation(project(":backend:components:log-component:message-log-component"))
    testRuntimeOnly("com.h2database:h2")

    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }
}
