import com.android.build.gradle.internal.dsl.BuildType
import io.sentry.android.gradle.SentryBuildTypeExtension
import org.gradle.api.plugins.ExtensionAware

val BuildType.sentry: SentryBuildTypeExtension
    get() = (this as ExtensionAware).extensions.getByName("sentry") as SentryBuildTypeExtension

fun BuildType.sentry(configure: SentryBuildTypeExtension.() -> Unit) {
    sentry.apply(configure)
}