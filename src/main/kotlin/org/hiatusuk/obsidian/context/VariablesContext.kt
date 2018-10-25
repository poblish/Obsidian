package org.hiatusuk.obsidian.context

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.MoreObjects
import com.google.common.base.Suppliers.memoize
import org.hiatusuk.obsidian.context.VariablesContext.MatchBehaviour.IGNORE_IF_MISSING
import org.hiatusuk.obsidian.context.el.FunctionsContext
import org.hiatusuk.obsidian.context.el.VariablesContextAwareElContext
import org.hiatusuk.obsidian.context.el.VariablesContextExpressionResolver
import org.hiatusuk.obsidian.context.el.TolerantRootPropertyResolver
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.state.SuppressedErrors
import org.hiatusuk.obsidian.utils.StringUtils
import org.slf4j.Logger
import java.math.BigInteger
import java.security.SecureRandom
import java.util.HashMap
import java.util.regex.Pattern
import javax.el.ExpressionFactory
import javax.inject.Inject
import kotlin.collections.LinkedHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@ScenarioScope
class VariablesContext @Inject
constructor(private val factory: ExpressionFactory,
            private val functions: FunctionsContext,
            private val runProperties: RunProperties,
            private val errorsState: SuppressedErrors,
            private val log: Logger) {

    private var requestIsInnValidateMode: Boolean = false  // Very ugly from concurrency POV

    private val scenarioConstants = LinkedHashMap<String, Any>()

    private val elCtxt: VariablesContextAwareElContext by lazy {
        functions.initialiseCtxt()
    }

    @VisibleForTesting
    fun clear() {
        scenarioConstants.clear()
        (elCtxt.elResolver as VariablesContextExpressionResolver).rootPropertyResolver.clearProperties()
    }

    fun store(inKey: String, inVal: Any) {
        var value = inVal

        if (value is String) {
            // Result is variable, so must store on write
            value = StringUtils.replace(value, "\${randomString()}", memoize { randomString })
            value = StringUtils.replace(value, "\${randomLong()}", memoize { java.lang.Long.toString(randomLong) })
            value = StringUtils.replace(value, "\${randomInt()}", memoize { Integer.toString(randomInt) })
        }

        scenarioConstants[inKey] = value

        updateElResolverProperty(inKey, value)
    }

    fun resolve(inValue: Any): Any? {
        return if (inValue is String) resolve(inValue as String?) else inValue
    }

    private fun initialResolve(inValue: String, inBehaviour: MatchBehaviour): String? {
        var s: String = inValue
        var startIdx = 0

        while (true) {
            val m = VARIABLE_MATCHER.matcher(s)
            if (!m.find(startIdx)) {
                break
            }

            val keyName = m.group(1)
            if (!scenarioConstants.containsKey(keyName)) {
                startIdx += keyName.length + 3  // Skip...
            } else {
                s = performReplacement(s, keyName, scenarioConstants[keyName])
                startIdx = 0
            }
        }

        return s
    }

    fun resolve(inValue: String?, inBehaviour: MatchBehaviour = MatchBehaviour.FAIL_IF_MISSING, inValidateMode: Boolean = false): String? {
        if (inValue == null || inValue.isEmpty()) {
            return /* Don't interfere... */ inValue
        }

        synchronized (EL_CTXT_LOCK) {  // Yuk FIXME => Protects TolerantRootPropertyResolver
            TolerantRootPropertyResolver.CURR_BEHAVIOUR = inBehaviour

            return try {
                getValueOfStringFunction(factory, inValue, inValidateMode)
            } catch (t: Throwable) {
                errorsState.onError(t)
                null // Gah!
            }
        }
    }

    private fun performReplacement(input: String, keyName: String, inReplacement: Any?): String {
        val replacementStr = inReplacement?.toString().orEmpty()
        // System.out.println("Replace '" + "${" + keyName + "}" + "' with '" + replacementStr + "'");
        return StringUtils.replace(input, "\${$keyName}", replacementStr)
        // System.out.println("=> s='" + s + "'");
    }

    enum class MatchBehaviour {
        FAIL_IF_MISSING, IGNORE_IF_MISSING, CLEAR_IF_MISSING
    }

    /**
     * Apply any overrides over the top of anything defined by 'set:'. These in turn can be overridden within scripts
     */
    fun beginScenarios() {
        if (runProperties.runtimeOverrides.isEmpty()) {
            return
        }

        log.debug("Applying variable overrides {} over {}", runProperties.runtimeOverrides, scenarioConstants)
        scenarioConstants.putAll(runProperties.runtimeOverrides)

        for ((key, value) in runProperties.runtimeOverrides) {
            updateElResolverProperty(key, value)
        }
    }

    private fun updateElResolverProperty(inKey: String, inVal: Any) {
        (elCtxt.elResolver as VariablesContextExpressionResolver).rootPropertyResolver.setProperty(inKey, inVal)
    }

    private fun getValueOfStringFunction(factory: ExpressionFactory, expression: String, inValidateMode: Boolean): String? {
        try {
            requestIsInnValidateMode = inValidateMode  // FIXME *surely* must be a better way (probably not)

            val expr = factory.createValueExpression(elCtxt, initialResolve(expression, IGNORE_IF_MISSING), String::class.java)

            if (expression.contains("inc(") || expression.contains("dec(")) {  // Special app-level locking for inc/dec() *only*
                synchronized (EL_CTXT_LOCK) {
                    CURRENT = this
                    return expr.getValue(elCtxt)?.toString()
                }
            }

            return expr.getValue(elCtxt)?.toString()
        }
        finally {
            requestIsInnValidateMode = false  // FIXME Not thread-safe, must be a better way
        }
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this).add("vars", scenarioConstants).toString()
    }

    fun push(newValues: Map<String, Any>): Map<String, Any> {
        val prevState = HashMap<String, Any>()

        for ((key, value) in newValues) {
            if (scenarioConstants.containsKey(key)) {
                prevState[key] = scenarioConstants[key]!!
            }
            scenarioConstants[key] = value
        }

        return prevState
    }

    fun pop(npreviousValues: Map<String, Any>) {
        for ((key, value) in npreviousValues) {
            scenarioConstants[key] = value
        }
    }

    fun remove(propertyName: String) {
        scenarioConstants.remove(propertyName)
        (elCtxt.elResolver as VariablesContextExpressionResolver).rootPropertyResolver.removeProperty(propertyName)
    }

    companion object {
        private val EL_CTXT_LOCK = ByteArray(0)  // Why static?!? Functions (that use us, like inc()) have to be static, even though we are a Singleton
        private val RANDOM = SecureRandom()  // Forced to make static so that EL can use it...

        private var CURRENT : VariablesContext? = null

        private val VARIABLE_MATCHER = Pattern.compile("\\$\\{([^\\}]*)\\}")

        private val randomString: String
            @JvmStatic
            @ExposedMethod(namespace = "")
            get() = BigInteger(64, RANDOM).toString(32)

        private// abs() is a bit iffy...
        val randomLong: Long
            @JvmStatic
            @ExposedMethod(namespace = "")
            get() = Math.abs(RANDOM.nextLong())

        private// abs() is a bit iffy...
        val randomInt: Int
            @JvmStatic
            @ExposedMethod(namespace = "")
            get() = Math.abs(RANDOM.nextInt())

        @JvmStatic
        @ExposedMethod(namespace = "", name = "inc")
        fun inc(name: String): Any {
            val obj = CURRENT!!.scenarioConstants[name]

            if (CURRENT!!.requestIsInnValidateMode) {
                return obj ?: 0
            }

            return getStoreIncrementedObj(name, obj, 1)
        }

        @JvmStatic
        @ExposedMethod(namespace = "", name = "dec")
        fun dec(name: String): Any {
            val obj = CURRENT!!.scenarioConstants[name]

            if (CURRENT!!.requestIsInnValidateMode) {
                return obj ?: 0
            }

            return getStoreIncrementedObj(name, obj, -1)
        }

        private fun getStoreIncrementedObj(name: String, value: Any?, inc: Int) : Any {
            return when (value) {
                is Double -> {
                    CURRENT!!.store(name, value + inc)
                    value + inc
                }
                is Int -> {
                    CURRENT!!.store(name, value + inc)
                    value + inc
                }
                else -> {
                    val newVal = value.toString().toDouble() + inc
                    CURRENT!!.store(name, newVal)
                    newVal
                }
            }
        }
    }
}
