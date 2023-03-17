package io.sentry.android.gradle.util

import java.util.UUID
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.services.BuildServiceRegistration
import org.gradle.api.services.BuildServiceRegistry

/*
 * Adapted from https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/internal/services/buildServices.k
 */

fun <ServiceT : BuildService<ParamsT>, ParamsT : BuildServiceParameters> getBuildService(
    buildServiceRegistry: BuildServiceRegistry,
    buildServiceClass: Class<ServiceT>
): Provider<ServiceT> {
    val serviceName = getBuildServiceName(buildServiceClass)
    return buildServiceRegistry.registerIfAbsent(serviceName, buildServiceClass) {
        throw IllegalStateException("Service $serviceName is not registered.")
    }
}

/*
 * Get build service name that works even if build service types come from different class loaders.
 * If the service name is the same, and some type T is defined in two class loaders L1 and L2. E.g.
 * this is true for composite builds and other project setups (see b/154388196).
 *
 * Registration of service may register (T from L1) or (T from L2). This means that querying it with
 * T from other class loader will fail at runtime. This method makes sure both T from L1 and T from
 * L2 will successfully register build services.
 */
fun getBuildServiceName(type: Class<*>): String = type.name + "_" + perClassLoaderConstant

/** Used to get unique build service name. Each class loader will initialize its own version. */
private val perClassLoaderConstant = UUID.randomUUID().toString()
