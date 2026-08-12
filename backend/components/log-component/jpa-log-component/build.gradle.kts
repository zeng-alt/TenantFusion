plugins {
    id("java-library")
}

description = "jpa-log-component"

dependencies {
    api(project(":backend:components:log-component:api-log-component"))
    api(project(":backend:components:domain-component"))
    implementation(project(":backend:components:log-component:core-log-component"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }
}
