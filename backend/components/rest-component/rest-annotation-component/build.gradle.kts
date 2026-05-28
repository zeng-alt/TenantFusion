plugins {
    id("java-library")
}

group = "com.github.zeng.alt"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
