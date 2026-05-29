plugins {
    id("java-library")
}

dependencies {
    api(project(":backend:components:core-component"))
    api("org.springframework.data:spring-data-commons")
    api("org.springframework.data:spring-data-jpa")
    api("jakarta.persistence:jakarta.persistence-api")
    api(rootProject.libs.querydsl.jpa)
    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }
}
