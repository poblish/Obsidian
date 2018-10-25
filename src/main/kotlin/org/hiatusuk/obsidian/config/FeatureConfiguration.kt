package org.hiatusuk.obsidian.config

@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class FeatureConfiguration(val value: String)