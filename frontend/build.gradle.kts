description = "frontend"

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val pnpmCommand = if (isWindows) "pnpm.cmd" else "pnpm"

tasks.register<Exec>("build") {
    group = "build"
    description = "Build the Vue frontend (pnpm run build)"
    workingDir = projectDir
    commandLine(pnpmCommand, "run", "build")

    // 增量构建配置：当这些文件未改变时跳过执行
    inputs.dir(file("src"))
    inputs.file("package.json")
    inputs.file("pnpm-lock.yaml")
    outputs.dir(file("dist"))
}

tasks.register<Exec>("run") {
    group = "application"
    description = "Run the Vue frontend in development mode (pnpm run dev)"
    workingDir = projectDir
    commandLine(pnpmCommand, "run", "dev")
}

tasks.register<Delete>("clean") {
    group = "build"
    description = "Clean the Vue frontend build output (dist)"
    delete("dist")
}
