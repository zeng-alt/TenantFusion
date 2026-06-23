plugins {
    id("java-library")
}

description = "cookie认证"

dependencies {
    api(project(":backend:components:security-component:core-security-component"))
    implementation(project(":backend:components:storage-component:api-storage-component"))
    implementation("org.springdoc:springdoc-openapi-starter-common")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
