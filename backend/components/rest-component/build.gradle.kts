plugins {
    id("java-library")
}

group = "com.github.zeng.alt"

dependencies {

}

tasks.withType<Jar> {
    enabled = false
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
