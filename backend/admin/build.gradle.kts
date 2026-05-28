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

                // 瀛楃�︾紪鐮佺浉鍏�
                "-Dfile.encoding=UTF-8",                 // 璁剧疆鏂囦欢缂栫爜
                "-Duser.country=CN",                     // 璁剧疆鍥藉��
                "-Duser.language=zh"                     // 璁剧疆璇�瑷�
            )
        }
    }
    metadataRepository {
        enabled.set(true)
        // 鍙�浠ユ寚瀹� metadata 浠撳簱鐗堟湰
        version.set("0.3.15") // 鎴栨洿楂?
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    outputs.upToDateWhen { false }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    // 鏈�鍦板紑鍙戣繍琛岋紙bootRun锛夋椂榛樿�や娇鐢� dev 閰嶇疆鏂囦欢
    systemProperty("spring.profiles.active", System.getProperty("spring.profiles.active", "dev"))
}

// 鎺ユ敹澶栭儴浼犲叆鐨?profiles.active 鍙傛暟锛岄粯璁や负 prod
val activeProfile = project.findProperty("profiles.active") as? String ?: "dev"

dependencies {
    // 鍙�浠ュ湪杩欓噷寮曞�?admin 妯″潡鐗规湁鐨勪緷璧栵紝渚嬪�� Web, JPA 绛?
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
    annotationProcessor(project(":backend:components:rest-component:rest-apt-component"))
}
