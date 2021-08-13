package com.blokkok.modsys.modinter.annotations

/**
 * Define a function has a blokkok module communication function
 *
 * Functions annotated with this annotation will get added automatically to the module namespace, or
 * the namespace its in if it's nested inside a `@Namespace` annotated `object` class.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Function(
    /**
     * The name that will be used as the communication function name
     */
    val name: String = "",

    /**
     * Parameter names mapping, you MUST use this when you use java to develop blokkok modules.
     *
     * these parameter names will be used to determine which parameter will be put in which index
     * of the function parameter
     *
     * Example usage:
     * ```java
     * @Function(paramNames = { "name", "age" })
     * public void doSomething(String name, int age) {
     *     /* do something */
     * }
     * ```
     */
    val paramNames: Array<String> = []
)
