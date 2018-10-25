package org.hiatusuk.obsidian.run.events

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy.RUNTIME

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(RUNTIME)
annotation class ApplicationQuit
