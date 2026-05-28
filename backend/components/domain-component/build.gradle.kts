plugins {  
    id("java-library")  
}  
  
dependencies {  
    api(project(":backend:components:core-component"))  
    api("org.springframework.data:spring-data-commons")
    api(libs.querydsl.jpa)
    annotationProcessor(libs.querydsl.apt)
} 
