package io.sentry.android.gradle.instrumentation.fakes

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.slf4j.Marker

abstract class BaseTestLogger : Logger {

    override fun isTraceEnabled(): Boolean = true

    override fun isTraceEnabled(marker: Marker): Boolean = true

    override fun trace(msg: String) = Unit

    override fun trace(msg: String, arg: Any) = Unit

    override fun trace(msg: String, arg: Any, arg2: Any) = Unit

    override fun trace(msg: String, vararg args: Any) = Unit

    override fun trace(msg: String, throwable: Throwable?) = Unit

    override fun trace(marker: Marker, msg: String) = Unit

    override fun trace(marker: Marker, msg: String, arg2: Any) = Unit

    override fun trace(marker: Marker, msg: String, arg2: Any, arg3: Any) = Unit

    override fun trace(marker: Marker, msg: String, vararg args: Any) = Unit

    override fun trace(marker: Marker, msg: String, throwable: Throwable?) = Unit

    override fun isDebugEnabled(): Boolean = true

    override fun isDebugEnabled(marker: Marker): Boolean = true

    override fun debug(msg: String) = Unit

    override fun debug(msg: String, arg: Any) = Unit

    override fun debug(msg: String, arg: Any, arg2: Any) = Unit

    override fun debug(msg: String, vararg args: Any) = Unit

    override fun debug(msg: String, throwable: Throwable?) = Unit

    override fun debug(marker: Marker, msg: String) = Unit

    override fun debug(marker: Marker, msg: String, arg2: Any) = Unit

    override fun debug(marker: Marker, msg: String, arg2: Any, arg3: Any) = Unit

    override fun debug(marker: Marker, msg: String, vararg args: Any) = Unit

    override fun debug(marker: Marker, msg: String, throwable: Throwable?) = Unit

    override fun isInfoEnabled(): Boolean = true

    override fun isInfoEnabled(marker: Marker): Boolean = true

    override fun info(msg: String) = Unit

    override fun info(msg: String, arg: Any) = Unit

    override fun info(msg: String, arg: Any, arg2: Any) = Unit

    override fun info(msg: String, vararg args: Any) = Unit

    override fun info(msg: String, throwable: Throwable?) = Unit

    override fun info(marker: Marker, msg: String) = Unit

    override fun info(marker: Marker, msg: String, arg2: Any) = Unit

    override fun info(marker: Marker, msg: String, arg2: Any, arg3: Any) = Unit

    override fun info(marker: Marker, msg: String, vararg args: Any) = Unit

    override fun info(marker: Marker, msg: String, throwable: Throwable?) = Unit

    override fun isWarnEnabled(): Boolean = true

    override fun isWarnEnabled(marker: Marker): Boolean = true

    override fun warn(msg: String) = Unit

    override fun warn(msg: String, arg: Any) = Unit

    override fun warn(msg: String, vararg args: Any) = Unit

    override fun warn(msg: String, arg: Any, arg2: Any) = Unit

    override fun warn(msg: String, throwable: Throwable?) = Unit

    override fun warn(marker: Marker, msg: String) = Unit

    override fun warn(marker: Marker, msg: String, arg2: Any) = Unit

    override fun warn(marker: Marker, msg: String, arg2: Any, arg3: Any) = Unit

    override fun warn(marker: Marker, msg: String, vararg args: Any) = Unit

    override fun warn(marker: Marker, msg: String, throwable: Throwable?) = Unit

    override fun isErrorEnabled(): Boolean = true

    override fun isErrorEnabled(marker: Marker): Boolean = true

    override fun error(msg: String) = Unit

    override fun error(msg: String, arg: Any) = Unit

    override fun error(msg: String, arg: Any, arg2: Any) = Unit

    override fun error(msg: String, vararg args: Any) = Unit

    override fun error(msg: String, throwable: Throwable?) = Unit

    override fun error(marker: Marker, msg: String) = Unit

    override fun error(marker: Marker, msg: String, arg2: Any) = Unit

    override fun error(marker: Marker, msg: String, arg2: Any, arg3: Any) = Unit

    override fun error(marker: Marker, msg: String, vararg args: Any) = Unit

    override fun error(marker: Marker, msg: String, throwable: Throwable?) = Unit

    override fun isLifecycleEnabled(): Boolean = true

    override fun lifecycle(message: String?) = Unit

    override fun lifecycle(message: String?, vararg objects: Any?) = Unit

    override fun lifecycle(message: String?, throwable: Throwable?) = Unit

    override fun isQuietEnabled(): Boolean = true

    override fun quiet(message: String?) = Unit

    override fun quiet(message: String?, vararg objects: Any?) = Unit

    override fun quiet(message: String?, throwable: Throwable?) = Unit

    override fun isEnabled(level: LogLevel?): Boolean = true

    override fun log(level: LogLevel?, message: String?) = Unit

    override fun log(level: LogLevel?, message: String?, vararg objects: Any?) = Unit

    override fun log(level: LogLevel?, message: String?, throwable: Throwable?) = Unit
}
