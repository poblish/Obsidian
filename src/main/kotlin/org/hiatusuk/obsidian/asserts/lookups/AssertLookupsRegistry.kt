package org.hiatusuk.obsidian.asserts.lookups

import com.codahale.metrics.MetricRegistry
import com.google.common.base.Throwables
import org.hiatusuk.obsidian.asserts.AssertTarget
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.di.includes.AssertLookupsComponent
import org.hiatusuk.obsidian.run.delegates.CommandNamespaces
import org.hiatusuk.obsidian.run.delegates.ReflectionsState
import org.hiatusuk.obsidian.run.events.ScenarioStartup
import org.slf4j.Logger
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject

@ScenarioScope
class AssertLookupsRegistry @Inject
constructor(private val lookups: AssertLookupsComponent,
            private val ns: CommandNamespaces, // FIXME Should probably rename if using for asserts too!
            private val reflections: ReflectionsState,
            private val metrics: MetricRegistry,
            private val log: Logger) {

    private val assertLookupMethodsCache: Array<Method> = lookups.javaClass.methods

    private val mappings = linkedMapOf<LookupPattern, Class<*>>()

    private var done: Boolean = false

    @ScenarioStartup
    fun startUp() {
        if (done) {
            return
        }

        val ref = reflections.all()  // Outside the timer...

        metrics.timer("AssertLookupsRegistry.generate").time().use {
            for (eachClass in ref.getTypesAnnotatedWith(AssertLookup::class.java)) {
                val ann = /* Command name: */ eachClass.getAnnotation(AssertLookup::class.java)
                val pattStr = ann.value
                val patt = if (ann.caseInsensitive) Pattern.compile("^$pattStr", Pattern.CASE_INSENSITIVE) else Pattern.compile("^$pattStr")
                if (mappings.put(LookupPattern(pattStr, ann.caseInsensitive, patt), eachClass) != null) {
                    throw RuntimeException("Duplicate mapping")
                }
            }

            log.trace("Derived: {}", mappings)
        }

        done = true
    }

    @Throws(IOException::class)
    fun lookupValueForTargetIdentifier(targetId: String, assertSpecs: AssertSpecs): Collection<AssertTarget>? {

        var lookupInst: Any? = null
        var lookupMethod: Method? = null
        var lookupExtraArgMethod: Method? = null
        var canonicalTargetId: String? = null

        metrics.timer("AssertLookupsRegistry.lookupForName").time().use {

            canonicalTargetId = ns.toCanonicalName(targetId)
            var clz: Class<*>? = null

            for ((lookupPattern, value) in mappings) {
                if (lookupPattern.matcher(canonicalTargetId).find()) {
                    clz = value
                    break
                }
            }

            // System.out.println("**** " + clz + " for " + targetId);

            for (eachMethod in assertLookupMethodsCache) {
                if (eachMethod.returnType == clz) {
                    try {
                        eachMethod.isAccessible = true
                        lookupInst = eachMethod.invoke(lookups)
                        if (lookupInst == null) {
                            throw RuntimeException("Instance not found")
                        }

                        try {
                            lookupExtraArgMethod = clz!!.getDeclaredMethod("lookup", String::class.java, AssertSpecs::class.java)
                            break
                        } catch (e: NoSuchMethodException) { /* Ignore */
                        }

                        lookupMethod = clz!!.getDeclaredMethod("lookup", String::class.java)
                        break

                    } catch (e: ReflectiveOperationException) {
                        throw RuntimeException(e)
                    }

                }
            }
        }

        try {
            if (lookupExtraArgMethod != null) {
                return lookupExtraArgMethod!!.invoke(lookupInst, canonicalTargetId, assertSpecs) as Collection<AssertTarget>
            }

            if (lookupMethod != null) {
                return lookupMethod!!.invoke(lookupInst, canonicalTargetId) as Collection<AssertTarget>
            }

            return null
        }
        catch (e: InvocationTargetException) {
            Throwables.propagateIfPossible(e.cause, IOException::class.java)
            throw RuntimeException(e.cause)
        }
        catch (t: Throwable) {
            Throwables.propagateIfPossible(t, IOException::class.java)
            Throwables.throwIfUnchecked(t)
            throw RuntimeException(t)
        }
    }

    // Deal with fact that we must use pattern string/insensitive flag for hash/equals, but need compiled Pattern associated with it
    class LookupPattern(private val pattStr: String,
                        private val insensitive: Boolean,
                        private val compiledPattern: Pattern) {

        fun matcher(str: String?): Matcher {
            return compiledPattern.matcher(str!!)
        }

        override fun hashCode() : Int {
            return Objects.hash(pattStr, insensitive)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LookupPattern
            return Objects.equals(pattStr, other.pattStr) && insensitive == other.insensitive
        }
    }
}
