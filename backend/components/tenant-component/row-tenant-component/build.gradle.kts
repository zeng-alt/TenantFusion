plugins {
    id("java-library")
}

description = "row-tenant-component"

dependencies {
    api(project(":backend:components:tenant-component:core-tenant-component"))
    api(project(":backend:components:domain-component"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // BaseEntity 实现了 Auditable，而 domain-component 把 spring-data-commons 声明为
    // implementation，未传递到下游编译类路径，这里本地补上
    implementation("org.springframework.data:spring-data-commons")
    compileOnly(rootProject.libs.lombok)
    annotationProcessor(rootProject.libs.lombok)
    // TenantBaseEntity 是 @MappedSuperclass，下游实体生成的 Q 类会引用 QTenantBaseEntity，
    // 因此本模块必须自己跑 querydsl-apt，否则 admin 编译时找不到该符号
    annotationProcessor(rootProject.libs.querydsl.apt) {
        artifact {
            classifier = "jakarta"
        }
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation(rootProject.libs.h2)
}
