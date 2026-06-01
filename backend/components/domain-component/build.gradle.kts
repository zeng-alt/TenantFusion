plugins {
    id("java-library")
}

dependencies {
    api(project(":backend:components:core-component"))
    api("jakarta.persistence:jakarta.persistence-api")
    api("org.hibernate.orm:hibernate-core")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.data:spring-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    api(rootProject.libs.querydsl.jpa)
    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }
}
