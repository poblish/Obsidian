package org.hiatusuk.obsidian.utils

import org.apache.commons.validator.routines.ISBNValidator
import org.apache.commons.validator.routines.UrlValidator
import org.hiatusuk.obsidian.context.ExposedMethod

@Suppress("unused")
class ExposedUtilityFunctions {

    companion object {

        @JvmStatic
        val currentTime: Long
            @ExposedMethod(namespace = "time", name = "current")
            get() = currentTimeMillis / 1000L

        // Could have used nanos...
        @JvmStatic
        val currentTimeMillis: Long
            @ExposedMethod(namespace = "time", name = "currentMillis")
            get() = System.currentTimeMillis()

        @JvmStatic
        val nanos: Long
            @ExposedMethod(namespace = "time", name = "nanos")
            get() = System.nanoTime()

        @JvmStatic
        val currentThreadName: String
            @ExposedMethod(namespace = "", name = "thread")
            get() = Thread.currentThread().name

        @JvmStatic
        @ExposedMethod(namespace = "env", name = "properties")
        fun getSystemProperties(): Any {
            return System.getProperties().toString()
        }

        @JvmStatic
        @ExposedMethod(namespace = "env", name = "property")
        fun getSystemProperty(propertyName: String?): Any {
            return System.getProperty(propertyName, "")
        }

        @JvmStatic
        @ExposedMethod(namespace = "env", name = "hasProperty")
        fun hasSystemProperty(propertyName: String?): Any {
            return System.getProperty(propertyName) != null
        }

        @JvmStatic
        @ExposedMethod(namespace = "env", name = "envs")
        fun envs(): Map<String,String> {
            return System.getenv()
        }

        @JvmStatic
        @ExposedMethod(namespace = "env", name = "env")
        fun env(varName: String?): String {
            return System.getenv().getOrDefault(varName, "")
        }

        @JvmStatic
        @ExposedMethod(namespace = "env", name = "hasEnv")
        fun hasEnv(varName: String?): Any {
            return System.getenv().containsKey(varName)
        }

        @JvmStatic
        @ExposedMethod(namespace = "commons", name = "validate")
        fun validate(validationType: String, inArg: Any): Boolean {
            val stringArg = requireNotNull(inArg) {"Argument cannot be null"}.toString()

            // Special cases, special options...
            if (validationType.equals("url", ignoreCase = true)) {
                return UrlValidator(UrlValidator.ALLOW_LOCAL_URLS).isValid(stringArg)
            }
            if (validationType.equals("isbn", ignoreCase = true)) {
                return ISBNValidator.getInstance().isValid(stringArg)
            }

            ///////////////////////////////////////////////////////////

            val clazz = Class.forName("org.apache.commons.validator.routines." + StringUtils.titleCase(validationType) + "Validator")
            val validator = try {
                // Ugh, how about a common API, or shared interface??
                clazz.getMethod("getInstance").invoke(null)
            } catch (e: NoSuchMethodException) {
                clazz.newInstance()
            }

            val m = clazz.getMethod("isValid", String::class.java)
            return m.invoke(validator, stringArg) as Boolean
        }
    }
}
