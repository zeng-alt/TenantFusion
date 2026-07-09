plugins {
    id("java-library")
}

description = "领域模型基础：JPA 实体、Repository、QueryDSL 支持等"

dependencies {
    api(project(":backend:components:core-component"))
    api("jakarta.persistence:jakarta.persistence-api")
    api("org.hibernate.orm:hibernate-core")
    api(rootProject.libs.jpa.search.helper)
    api(project(":backend:components:rest-component:rest-annotation-component"))
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
