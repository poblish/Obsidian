package org.hiatusuk.obsidian.context.el

import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.context.VariablesContext.MatchBehaviour
import java.beans.FeatureDescriptor
import java.util.*
import javax.el.ELContext
import javax.el.ELResolver
import javax.el.PropertyNotFoundException
import javax.el.PropertyNotWritableException

// Basically a copy/paste from RootPropertyResolver, but with extra logic in 'getValue'
class TolerantRootPropertyResolver

@JvmOverloads constructor(private val readOnly: Boolean = false) : ELResolver() {
    private val map = Collections.synchronizedMap(HashMap<String, Any>())

    private fun isResolvable(base: Any?): Boolean {
        return base == null
    }

    private fun resolve(context: ELContext, base: Any?, property: Any?): Boolean {
        context.isPropertyResolved = isResolvable(base) && property is String
        return context.isPropertyResolved
    }

    override fun getCommonPropertyType(context: ELContext, base: Any): Class<*>? {
        return if (isResolvable(context)) String::class.java else null
    }

    override fun getFeatureDescriptors(context: ELContext, base: Any): Iterator<FeatureDescriptor>? {
        return null
    }

    override fun getType(context: ELContext, base: Any, property: Any): Class<*>? {
        return if (resolve(context, base, property)) Any::class.java else null
    }

    override fun getValue(context: ELContext, base: Any?, property: Any): Any? {
        if (resolve(context, base, property)) {
            if (!isProperty(property as String)) {
                if (CURR_BEHAVIOUR == null || CURR_BEHAVIOUR == MatchBehaviour.FAIL_IF_MISSING) {
                    throw PropertyNotFoundException("Cannot find property $property")
                }
                return null // throw new PropertyNotFoundException("Cannot find property " + property);
            }
            return getProperty(property)
        }
        return null
    }

    override fun isReadOnly(context: ELContext, base: Any, property: Any): Boolean {
        return resolve(context, base, property) && readOnly
    }

    @Throws(PropertyNotWritableException::class)
    override fun setValue(context: ELContext, base: Any, property: Any, value: Any) {
        throw UnsupportedOperationException("Not used or supported any more")
    }

    override fun invoke(context: ELContext?, base: Any?, method: Any?, paramTypes: Array<Class<*>>?, params: Array<Any>?): Any? {
        if (resolve(context!!, base, method)) {
            throw NullPointerException("Cannot invoke method $method on null")
        }
        return null
    }

    /**
     * Get property value
     *
     * @param property
     * property name
     * @return value associated with the given property
     */
    private fun getProperty(property: String): Any? {
        return map[property]
    }

    /**
     * Set property value
     *
     * @param property
     * property name
     * @param value
     * property value
     */
    fun setProperty(property: String, value: Any) {
        map[property] = value
    }

    fun removeProperty(property: String) {
        map.remove(property)
    }

    /**
     * Test property
     *
     * @param property
     * property name
     * @return `true` if the given property is associated with a value
     */
    private fun isProperty(property: String): Boolean {
        return map.containsKey(property)
    }

    fun clearProperties() {
        map.clear()
    }

    companion object {

        var CURR_BEHAVIOUR: VariablesContext.MatchBehaviour? = null
    }
}