description = "admin-application"
group = "com.github.zeng.alt"
version = "0.0.1-SNAPSHOT"

plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.hibernate.orm")
    id("org.graalvm.buildtools.native")
}


graalvmNative {
    binaries {
        named("main") {
            buildArgs.addAll(
                "-H:-CheckToolchain",
                "--no-fallback",
                "--install-exit-handlers",
                "--enable-url-protocols=http,https",

                // 鐎涙锟斤妇绱惍浣烘祲閸忥拷
                "-Dfile.encoding=UTF-8",                 // 鐠佸墽鐤嗛弬鍥︽缂傛牜鐖?
                "-Duser.country=CN",                     // 鐠佸墽鐤嗛崶钘夛拷锟?
                "-Duser.language=zh"                     // 鐠佸墽鐤嗙拠锟界懛锟?
            )
        }
    }
    metadataRepository {
        enabled.set(true)
        // 閸欙拷娴犮儲瀵氱€癸拷 metadata 娴犳挸绨遍悧鍫熸拱
        version.set("0.3.15") // 閹存牗娲挎?
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    outputs.upToDateWhen { false }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    // 閺堬拷閸︽澘绱戦崣鎴ｇ箥鐞涘矉绱檅ootRun閿涘妞傛妯匡拷銈勫▏閻拷 dev 闁板秶鐤嗛弬鍥︽
    systemProperty("spring.profiles.active", System.getProperty("spring.profiles.active", "dev"))
}

// 閹恒儲鏁规径鏍劥娴肩姴鍙嗛惃?profiles.active 閸欏倹鏆熼敍宀勭帛鐠併倓璐?prod
val activeProfile = project.findProperty("profiles.active") as? String ?: "dev"

dependencies {
    // 閸欙拷娴犮儱婀潻娆撳櫡瀵洖锟?admin 濡€虫健閻楄婀侀惃鍕贩鐠ф牭绱濇笟瀣拷锟?Web, JPA 缁?
    if (activeProfile != "prod") {
        implementation(libs.liquibase.core)
    }
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.h2)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.starter.jpa)
    compileOnly(libs.lombok)
    developmentOnly(libs.spring.boot.docker.compose)
    implementation(libs.spring.boot.starter.data.redis)
    annotationProcessor(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.lombok)
    annotationProcessor("com.squareup:javapoet:1.13.0")
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.modulith.starter.test)
    testImplementation(libs.spring.security.test)
    testCompileOnly(libs.lombok)
    testRuntimeOnly(libs.junit.platform.launcher)
    testAnnotationProcessor(libs.lombok)

    implementation(project(":backend:components:rest-component:rest-annotation-component"))
    implementation(project(":backend:components:core-component"))
    implementation(project(":backend:components:domain-component"))
    implementation(project(":backend:components:i18n-component"))
    annotationProcessor(project(":backend:components:rest-component:rest-apt-component"))
}
