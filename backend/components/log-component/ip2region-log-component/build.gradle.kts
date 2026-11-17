plugins {
    id("java-library")
}

description = "使用 ip2region 离线查询日志 IP 归属地"

dependencies {
    api(project(":backend:components:log-component:core-log-component"))
    implementation(rootProject.libs.ip2region)
    implementation(rootProject.libs.vavr)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-jackson")
}
