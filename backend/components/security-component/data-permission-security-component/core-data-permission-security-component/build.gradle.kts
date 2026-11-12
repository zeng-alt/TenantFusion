plugins { id("java-library"); jacoco }

description = "数据权限规则解析和范围合并"
dependencies {
    api(project(":backend:components:security-component:data-permission-security-component:api-data-permission-security-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.spring.boot.configuration.processor)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
