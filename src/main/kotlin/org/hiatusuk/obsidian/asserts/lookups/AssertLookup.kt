package org.hiatusuk.obsidian.asserts.lookups

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class AssertLookup(val value: String,  // command name binding
                              val caseInsensitive: Boolean = false)
