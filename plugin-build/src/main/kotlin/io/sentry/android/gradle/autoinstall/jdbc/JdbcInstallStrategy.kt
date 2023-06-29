package io.sentry.android.gradle.autoinstall.jdbc

import io.sentry.android.gradle.SentryPlugin
import io.sentry.android.gradle.autoinstall.AbstractInstallStrategy
import io.sentry.android.gradle.autoinstall.AutoInstallState
import io.sentry.android.gradle.autoinstall.InstallStrategyRegistrar
import io.sentry.android.gradle.util.SemVer
import javax.inject.Inject
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.slf4j.Logger

// @CacheableRule
abstract class JdbcInstallStrategy : AbstractInstallStrategy {

    constructor(logger: Logger) : super() {
        this.logger = logger
    }

    @Suppress("unused") // used by Gradle
    @Inject // inject is needed to avoid Gradle error
    constructor() : this(SentryPlugin.logger)

    override val sentryModuleId: String get() = SENTRY_JDBC_ID

    override val shouldInstallModule: Boolean get() = AutoInstallState.getInstance().installJdbc

    override val minSupportedThirdPartyVersion: SemVer get() = MIN_SUPPORTED_VERSION

    override val minSupportedSentryVersion: SemVer get() = SemVer(4, 4, 0)

    companion object Registrar : InstallStrategyRegistrar {
        private const val SPRING_JDBC_GROUP = "org.springframework"
        private const val SPRING_JDBC_ID = "spring-jdbc"

        private const val HSQL_GROUP = "org.hsqldb"
        private const val HSQL_ID = "hsqldb"

        private const val MYSQL_GROUP = "mysql"
        private const val MYSQL_ID = "mysql-connector-java"

        private const val MARIADB_GROUP = "org.mariadb.jdbc"
        private const val MARIADB_ID = "mariadb-java-client"

        private const val POSTGRES_GROUP = "org.postgresql"
        private const val POSTGRES_ID = "postgresql"

        private const val ORACLE_GROUP = "com.oracle.jdbc"
        private const val ORACLE_DATABASE_GROUP = "com.oracle.datqbase.jdbc"
        private const val ORACLE_OJDBC_ID_PREFIX = "ojdbc"

        internal const val SENTRY_JDBC_ID = "sentry-jdbc"

        private val MIN_SUPPORTED_VERSION = SemVer(1, 0, 0)

        override fun register(component: ComponentMetadataHandler) {
            component.withModule(
                "$SPRING_JDBC_GROUP:$SPRING_JDBC_ID",
                JdbcInstallStrategy::class.java
            ) {}
            component.withModule(
                "$HSQL_GROUP:$HSQL_ID",
                JdbcInstallStrategy::class.java
            ) {}
            component.withModule(
                "$MYSQL_GROUP:$MYSQL_ID",
                JdbcInstallStrategy::class.java
            ) {}
            component.withModule(
                "$MARIADB_GROUP:$MARIADB_ID",
                JdbcInstallStrategy::class.java
            ) {}
            component.withModule(
                "$POSTGRES_GROUP:$POSTGRES_ID",
                JdbcInstallStrategy::class.java
            ) {}

            (5..14).forEach {
                println("$ORACLE_GROUP:$ORACLE_OJDBC_ID_PREFIX$it")

                component.withModule(
                    "$ORACLE_GROUP:$ORACLE_OJDBC_ID_PREFIX$it",
                    JdbcInstallStrategy::class.java
                ) {}

                component.withModule(
                    "$ORACLE_DATABASE_GROUP:$ORACLE_OJDBC_ID_PREFIX$it",
                    JdbcInstallStrategy::class.java
                ) {}
            }
            }
    }
}
