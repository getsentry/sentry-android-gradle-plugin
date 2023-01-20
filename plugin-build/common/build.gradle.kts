plugins {
    kotlin("jvm") version BuildPluginsVersion.KOTLIN
    id("com.vanniktech.maven.publish") version BuildPluginsVersion.MAVEN_PUBLISH apply false
}

dependencies {
    compileOnly(Libs.GRADLE_API)
}

//val sep = File.separator
//
//distributions {
//    main {
//        contents {
//            from("build${sep}libs")
//            from("build${sep}publications${sep}maven")
//        }
//    }
//}
//
//apply {
//    plugin("com.vanniktech.maven.publish")
//}
//
//val publish = extensions.getByType(MavenPublishPluginExtension::class.java)
//// signing is done when uploading files to MC
//// via gpg:sign-and-deploy-file (release.kts)
//publish.releaseSigningEnabled = false
//
//tasks.named("distZip") {
//    dependsOn("publishToMavenLocal")
//    onlyIf {
//        inputs.sourceFiles.isEmpty.not().also {
//            require(it) { "No distribution to zip." }
//        }
//    }
//}
