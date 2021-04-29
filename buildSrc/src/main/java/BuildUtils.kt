object BuildUtils {
    fun shouldSignArtifacts(): Boolean = !(System.getenv("CI")?.toBoolean() ?: false)
}
