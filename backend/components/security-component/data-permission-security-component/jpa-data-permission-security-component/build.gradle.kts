plugins { id("java-library"); jacoco }

description = "数据权限 JPA 和 Querydsl 查询适配"
dependencies {
    implementation("org.springframework.boot:spring-boot-hibernate")
    api(project(":backend:components:security-component:data-permission-security-component:core-data-permission-security-component"))
    api(project(":backend:components:domain-component"))
    api("org.springframework.data:spring-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework:spring-aop")
    implementation("org.aspectj:aspectjweaver")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testRuntimeOnly("com.h2database:h2")
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
