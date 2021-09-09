package io.sentry.android.gradle.instrumentation

interface Instrumentable<Visitor> {

    /**
     * Fully-qualified name of the instrumentable. Examples:
     * Class: androidx.sqlite.db.framework.FrameworkSQLiteDatabase
     * Method: query
     */
    val fqName: String

    /**
     * Provides a visitor for this instrumentable. A visitor can be one of the visitors defined
     * in [ASM](https://asm.ow2.io/javadoc/org/objectweb/asm/package-summary.html)
     *
     * @param apiVersion Defines the ASM api version, usually provided from the parent
     * @param originalVisitor The original visitor that ASM provides us with before visiting code
     * @param descriptor A descriptor of a class/method/field/etc. Useful, e.g. when you need to return
     * a different visitor for different method overloads.
     * @param debug Enables debug output. Defines whether the visitors should show the detailed report
     * while visiting. Defaults to false
     */
    fun getVisitor(
        apiVersion: Int,
        originalVisitor: Visitor,
        descriptor: String? = null,
        parameters: SpanAddingClassVisitorFactory.SpanAddingParameters
    ): Visitor

    /**
     * Provides children instrumentables that are going to be used when visiting the current
     * class/method/field/etc.
     */
    val children: List<Instrumentable<*>> get() = emptyList()
}
