package com.blokkok.modsys.annotations.processor

/**
 * This exception will be thrown if something goes wrong in the runtime annotation processing of
 * blokkok modules
 */
class AnnotationProcessingException(
    text: String
) : Exception("The module runtime annotation processor failed to process because $text")