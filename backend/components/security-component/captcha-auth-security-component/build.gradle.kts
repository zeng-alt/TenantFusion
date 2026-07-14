plugins {
    id("java-library")
}

description = "验证码认证"

dependencies {
    api(project(":backend:components:security-component:core-security-component"))
    api(project(":backend:components:captcha-component"))
}
