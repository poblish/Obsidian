package org.hiatusuk.obsidian.context.el

import com.codahale.metrics.MetricRegistry
import org.hiatusuk.obsidian.context.ExposedMethod
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.delegates.ReflectionsState
import org.hiatusuk.obsidian.utils.StringUtils
import org.slf4j.Logger
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import javax.inject.Inject

// TODO Most of this really should be static / @Singleton, but VCAEL has to be @SecenarioScope
@ScenarioScope
class FunctionsContext
@Inject constructor(private val vcael: VariablesContextAwareElContext,
                    private val reflections: ReflectionsState,
                    private val log: Logger,
                    private val metrics: MetricRegistry) {

    @Volatile
    private var inited = false

    fun initialiseCtxt(): VariablesContextAwareElContext {
        if (!inited) {
            synchronized(this) {
                doInitialiseCtxt(vcael)
                inited = true
            }
        }
        return vcael
    }

    private fun doInitialiseCtxt(el: VariablesContextAwareElContext): VariablesContextAwareElContext {

        // Why not randomString() here too? Because it's non-deterministic, and must be *locked down* at store time
        el.setFunction("math", "max", Math::class.java.getMethod("max", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType))
        el.setFunction("math", "min", Math::class.java.getMethod("min", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType))
        el.setFunction("math", "rint", Math::class.java.getMethod("rint", Double::class.javaPrimitiveType))
        el.setFunction("math", "floor", Math::class.java.getMethod("floor", Double::class.javaPrimitiveType))

        metrics.timer("FunctionsContext.registerExposedMethods").time().use {

            val potentialMethods : Set<Method> = reflections.all().getMethodsAnnotatedWith(ExposedMethod::class.java)
            val successfulMethodNames = arrayListOf<String>()

            // Loop through all the static methods, building up a list of names of those that are appropriate.
            // In Kotlin with @JvmStatic we seem to get two methods per annotation, so we only want to WARN against
            // those methods that are both non-static *AND* don't have the same name (i.e. mirror) an existing good one
            for (exposedMethod in potentialMethods.filter { m -> Modifier.isStatic(m.modifiers) }) {

                if (!exposedMethod.isAccessible) {
                    exposedMethod.isAccessible = true
                }

                val ann = exposedMethod.getAnnotation(ExposedMethod::class.java)

                if (ann.name.isEmpty() && /* Lame: */ !exposedMethod.name.startsWith("get")) {
                    throw RuntimeException("@ExposedMethod has no name attribute, and doesn't seem to be a getter")
                }

                successfulMethodNames.add(exposedMethod.name)

                val fixedName = if (ann.name.isEmpty()) StringUtils.camelCase(exposedMethod.name.substring(3)) else ann.name

                if (ann.namespace.isEmpty()) {
                    el.setFunction(fixedName!!, exposedMethod)
                } else {
                    el.setFunction(ann.namespace, fixedName!!, exposedMethod)
                }
            }

            // Now go through to identify which unusable entries are actually worth notifying about...
            for (unusableMethod in potentialMethods.filter { m -> !Modifier.isStatic(m.modifiers) && m.name !in successfulMethodNames }) {
                log.warn("@ExposedMethod '{}' needs to be static!", unusableMethod)
            }
        }

        return el
    }
}