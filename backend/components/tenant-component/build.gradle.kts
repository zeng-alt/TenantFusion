plugins {
    id("java-library")
}

description = "多租户组件"

dependencies {
    api(project(":backend:components:core-component"))
    api(project(":backend:components:domain-component"))
}
