package org.hiatusuk.obsidian.context.el

import java.beans.FeatureDescriptor
import javax.el.*

/**
 * Create a resolver capable of resolving top-level identifiers. Everything else is passed to
 * the supplied delegate.
 */
class VariablesContextExpressionResolver
@JvmOverloads constructor(resolver: ELResolver = DEFAULT_RESOLVER_READ_WRITE, readOnly: Boolean = false) : ELResolver() {

    /**
     * Answer our root resolver which provides an API to access top-level properties.
     *
     * @return root property resolver
     */
    val rootPropertyResolver: TolerantRootPropertyResolver = TolerantRootPropertyResolver(readOnly)
    private val delegate: CompositeELResolver = CompositeELResolver()

    init {
        delegate.add(rootPropertyResolver)
        delegate.add(resolver)
    }

    override fun getCommonPropertyType(context: ELContext, base: Any): Class<*> {
        return delegate.getCommonPropertyType(context, base)
    }

    override fun getFeatureDescriptors(context: ELContext, base: Any): Iterator<FeatureDescriptor> {
        return delegate.getFeatureDescriptors(context, base)
    }

    override fun getType(context: ELContext, base: Any, property: Any): Class<*> {
        return delegate.getType(context, base, property)
    }

    override fun getValue(context: ELContext, base: Any?, property: Any): Any? {
        return delegate.getValue(context, base, property)
    }

    override fun isReadOnly(context: ELContext, base: Any, property: Any): Boolean {
        return delegate.isReadOnly(context, base, property)
    }

    override fun setValue(context: ELContext, base: Any, property: Any, value: Any) {
        throw UnsupportedOperationException("Not used or supported any more")
    }

    @JvmSynthetic
    override fun invoke(context: ELContext?, base: Any?, method: Any?, paramTypes: Array<Class<*>>?, params: Array<Any>?): Any? {
        return delegate.invoke(context!!, base, method, paramTypes, params)
    }

    companion object {
        private val DEFAULT_RESOLVER_READ_WRITE = object : CompositeELResolver() {
            init {
                add(ArrayELResolver(false))
                add(ListELResolver(false))
                add(MapELResolver(false))
                add(ResourceBundleELResolver())
                add(BeanELResolver(false))
            }
        }
    }
}/**
 * Create a read/write resolver capable of resolving top-level identifiers, array values, list
 * values, map values, resource values and bean properties.
 */
