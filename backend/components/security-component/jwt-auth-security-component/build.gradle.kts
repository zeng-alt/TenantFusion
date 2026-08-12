plugins {
    id("java-library")
}

description = "jwt认证"

dependencies {
    api(project(":backend:components:security-component:core-security-component"))
    implementation(project(":backend:components:storage-component:api-storage-component"))
    implementation(project(":backend:components:log-component:api-log-component"))
    implementation("org.springdoc:springdoc-openapi-starter-common")

    api("org.springframework.security:spring-security-oauth2-jose")
//    api(libs.jjwt.api)
//    api(libs.jjwt.impl)
//    api(libs.jjwt.jackson)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
