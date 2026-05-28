plugins {  
    id("java-library")  
}  
  
dependencies {  
    api(project(":backend:components:core-component"))  
    api("org.springframework.data:spring-data-commons")  
} 
