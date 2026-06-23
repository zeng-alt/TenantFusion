plugins {
    id("java-library")
}

description = "jwt认证"

dependencies {
    api(project(":backend:components:security-component:core-security-component"))
    implementation(project(":backend:components:storage-component:api-storage-component"))

    api(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
