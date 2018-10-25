package org.hiatusuk.obsidian.cmd.api

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class Command(val value: String,  // command name binding
                         val regex: Boolean = false)
