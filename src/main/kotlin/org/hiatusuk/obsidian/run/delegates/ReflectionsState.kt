package org.hiatusuk.obsidian.run.delegates

import com.codahale.metrics.MetricRegistry
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import javax.inject.Inject

// Do not need Scope here
class ReflectionsState @Inject
constructor(private val metrics: MetricRegistry) {

    @Volatile
    private var reflections: Reflections? = null

    // Double-checked locking!
    fun all(): Reflections {
        if (reflections != null) {
            return reflections!!
        }

        synchronized(this) {
            metrics.timer("ReflectionsState.all").time().use {
                reflections = Reflections("org.hiatusuk.obsidian", TypeAnnotationsScanner(), SubTypesScanner(), MethodAnnotationsScanner()) }
            return reflections!!
        }
    }
}
