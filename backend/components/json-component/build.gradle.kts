plugins {  
    id("java-library")  
}  
  
dependencies {
    api(project(":backend:components:api-component"))
    api(libs.vavr.jackson)
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
