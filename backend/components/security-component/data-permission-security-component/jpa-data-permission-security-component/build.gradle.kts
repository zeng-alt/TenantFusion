plugins { id("java-library"); jacoco }

description = "数据权限 JPA 和 Querydsl 查询适配"
dependencies {
    implementation("org.springframework.boot:spring-boot-hibernate")
    api(project(":backend:components:security-component:data-permission-security-component:core-data-permission-security-component"))
    api(project(":backend:components:domain-component"))
    api("org.springframework.data:spring-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
}
