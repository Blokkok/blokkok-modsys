package com.blokkok.modsys.modinter.annotations

/**
 * Define a class (java) / an object (kotlin) as a namespace.
 *
 * Classes / Objects that are subclasses of a Module class that has this annotation will get added
 * as a Namespace automatically by the Module Runtime Annotation Processor
 */
@Target(AnnotationTarget.CLASS)
annotation class Namespace(
    /**
     * The name that will be used for the namespace.
     * If left empty, will use the object / class simple name instead
     */
    val name: String = "",
)
