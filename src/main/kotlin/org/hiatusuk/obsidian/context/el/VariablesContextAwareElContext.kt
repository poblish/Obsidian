package org.hiatusuk.obsidian.context.el

import org.hiatusuk.obsidian.di.ScenarioScope
import java.lang.reflect.Method
import java.util.*
import javax.el.*
import javax.inject.Inject

@ScenarioScope
class VariablesContextAwareElContext
@Inject constructor() : ELContext() {

    private val resolver = VariablesContextExpressionResolver()
    private val functions = Functions()

    override fun getELResolver(): ELResolver {
        return resolver
    }

    override fun getFunctionMapper(): FunctionMapper {
        return functions
    }

    fun setFunction(prefix: String, localName: String, method: Method) {
        functions.setFunction("$prefix:$localName", method)
    }

    fun setFunction(localName: String, method: Method) {
        functions.setFunction(localName, method)
    }

    internal class Functions : FunctionMapper() {
        var map: MutableMap<String, Method> = mutableMapOf()

        override fun resolveFunction(prefix: String, localName: String): Method? {
            return map[if (prefix.isEmpty()) localName else "$prefix:$localName"]
        }

        fun setFunction(fullName: String, method: Method) {
            if (map.isEmpty()) {
                map = HashMap()
            }
            map[fullName] = method
        }
    }

    override fun getVariableMapper(): VariableMapper {
        return MyVariableMapper()
    }

    private class MyVariableMapper : VariableMapper() {

        override fun resolveVariable(name: String): ValueExpression? {
            return null  // Not supported
        }

        override fun setVariable(name: String, expr: ValueExpression): ValueExpression? {
            throw UnsupportedOperationException()
        }
    }
}
