plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
}

if(shouldDownloadSentryCli()) {
    logger.lifecycle("Downloading Sentry CLI...")
    exec {
        executable("sh")
        workingDir("../plugin-build")
        args("-c", "./download-sentry-cli.sh")
    }
}

/**
 * When bumping the Sentry CLI, you should update the `expected-checksums.sha` file inside `buildSrc`
 * to match the `checksums.sha` file from `./plugin-build/src/main/resources/bin/`
 *
 * That's to retrigger a download of the cli upon a bump.
 */
fun shouldDownloadSentryCli() : Boolean {
    val cliDir: Array<File> = File("./plugin-build/src/main/resources/bin/").listFiles() ?: emptyArray()
    val expectedChecksums = File("./buildSrc/expected-checksums.sha")
    val actualChecksums = File("./plugin-build/src/main/resources/bin/checksums.sha")
    return when {
        cliDir.size <= 2 -> {
            logger.lifecycle("Sentry CLI is missing")
            true
        }
        !actualChecksums.exists() -> {
            logger.lifecycle("Sentry CLI Checksums is missing")
            true
        }
        expectedChecksums.readText() != actualChecksums.readText() -> {
            logger.lifecycle("Sentry CLI Checksums doesn't match")
            true
        }
        else -> false
    }
}
