description = "frontend"

val npmCmd = if (System.getProperty("os.name").lowercase().contains("windows")) "pnpm.cmd" else "pnpm"

tasks.register<Exec>("build") {
    group = "build"
    description = "Build the Vue frontend (pnpm run build)"
    workingDir = projectDir
    commandLine(npmCmd, "run", "build")

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
    commandLine(npmCmd, "run", "dev")
}
