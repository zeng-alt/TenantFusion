plugins {
    id("java-library")
}

description = "Excel 导入导出组件"

dependencies {
    api(project(":backend:components:api-component"))
    implementation(project(":backend:components:core-component"))
    implementation(project(":backend:components:i18n-component"))

    api(rootProject.libs.fesod.sheet)
    api(rootProject.libs.vavr)
    api(rootProject.libs.rxjava3)

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation(rootProject.libs.commons.lang3)

    // Web 集成（@ExcelImport 参数解析、@ExcelExport 返回值处理）按需生效：
    // 非 Web 应用不需要 servlet / webmvc，故编译期可见、运行期条件装配
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("jakarta.validation:jakarta.validation-api")

    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation(rootProject.libs.spring.boot.starter.validation)
}
