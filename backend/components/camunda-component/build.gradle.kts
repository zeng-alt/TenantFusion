plugins {
    id("java-library")
}

description = "流程引擎"


dependencies {

//    api(project(":backend:components:security-component:core-security-component"))

    // 流程引擎
    api("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter:7.24.0")
    // 提供rest api操作接口
    api("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-rest:7.24.0")
    // Web管理平台
    implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-webapp:7.24.0")
    // 要使用连接器和 http 客户端，必须将 connect 依赖项和 http 客户端添加到 POM 文件中
    api("org.camunda.bpm:camunda-engine-plugin-connect:7.24.0")

    api("org.camunda.connect:camunda-connect-http-client:7.24.0")
    //  为了解析响应主体，最好同时包含Spin 插件。要添加 Spin 和 JSON
    api("org.camunda.bpm:camunda-engine-plugin-spin:7.24.0")

    api("org.camunda.spin:camunda-spin-dataformat-json-jackson:7.24.0")

    api("org.graalvm.polyglot:polyglot:24.2.1")
    api("org.graalvm.polyglot:js:24.2.1")
    api("org.graalvm.js:js-scriptengine:24.2.1")



    api("dev.bpm-crafters.process-engine-api:process-engine-api:1.7")
    // Source: https://mvnrepository.com/artifact/dev.bpm-crafters.process-engine-adapters/process-engine-adapter-camunda-platform-c7-embedded-spring-boot-starter
// 本地使用
//    api("dev.bpm-crafters.process-engine-adapters:process-engine-adapter-camunda-platform-c7-embedded-spring-boot-starter:2026.06.2")

    // =================================================================================
    // 其他服务使用
//    // implementation("org.camunda.bpm:camunda-external-task-client:7.24.0")

////    implementation("dev.bpm-crafters.process-engine-worker:process-engine-worker-spring-boot-starter:0.8.5")


//    // Source: https://mvnrepository.com/artifact/dev.bpm-crafters.process-engine-adapters/process-engine-adapter-camunda-platform-c7-remote-spring-boot-starter
// 远程调用
// implementation("dev.bpm-crafters.process-engine-adapters:process-engine-adapter-camunda-platform-c7-remote-spring-boot-starter:2026.06.2")
//    implementation("io.holunda.c7:c7-rest-client-spring-boot-starter-feign:2026.04.2")
//    implementation("org.camunda.bpm.springboot:camunda-bpm-spring-boot-starter-external-task-client:7.24.0")
//    implementation("io.holunda.c7:c7-rest-client-spring-boot-starter")
//    implementation("io.holunda.data:camunda-bpm-data:2026.04.2")

}

