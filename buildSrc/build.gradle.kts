plugins {
    `kotlin-dsl`
}
repositories {
    jcenter()
}

val cliDir: Array<File> = File("./plugin-build/src/main/resources/bin/").listFiles() ?: emptyArray()
if(cliDir.size == 1 && cliDir[0].name == ".gitignore") {
    logger.lifecycle("Sentry CLI is Missing - Downloading it...")
    exec {
        executable("sh")
        workingDir("../plugin-build")
        args("-c", "./download-sentry-cli.sh")
    }
}
