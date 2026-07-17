plugins {
    id("java-library")
}

description = "Excel 导入导出组件"

dependencies {
    implementation(project(":backend:components:core-component"))
    implementation(project(":backend:components:i18n-component"))
    api(rootProject.libs.fesod.sheet)
    implementation("io.reactivex.rxjava3:rxjava:3.1.10")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    api("jakarta.servlet:jakarta.servlet-api")
    implementation("org.springframework:spring-webmvc")
}
