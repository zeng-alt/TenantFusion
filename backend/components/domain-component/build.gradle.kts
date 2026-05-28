plugins {  
    id("java-library")  
}  
  
dependencies {  
    api(project(":backend:components:core-component"))  
    api("org.springframework.data:spring-data-commons")
    api("org.springframework.data:spring-data-jpa")
    api(libs.querydsl.jpa)
    annotationProcessor(libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    api("jakarta.persistence:jakarta.persistence-api")
}