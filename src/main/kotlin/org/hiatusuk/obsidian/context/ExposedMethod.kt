package org.hiatusuk.obsidian.context

import kotlin.annotation.AnnotationTarget.*

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
annotation class ExposedMethod(val namespace: String = "o" /* obsidian is too long */, val name: String = "")
