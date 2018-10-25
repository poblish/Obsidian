package org.hiatusuk.obsidian.cmd.api

import org.hiatusuk.obsidian.config.ConfigUtils
import org.hiatusuk.obsidian.context.VariablesContext
import org.hiatusuk.obsidian.context.VariablesContext.MatchBehaviour.CLEAR_IF_MISSING
import org.hiatusuk.obsidian.context.VariablesContext.MatchBehaviour.FAIL_IF_MISSING
import org.hiatusuk.obsidian.files.FileUtils
import org.hiatusuk.obsidian.run.delegates.CommandNamespaces
import org.hiatusuk.obsidian.utils.Duration
import org.hiatusuk.obsidian.utils.IOUtils
import org.hiatusuk.obsidian.utils.ParseUtils
import java.io.File
import java.util.*
import kotlin.collections.Map.Entry

class CommandSpec {

    private val ns: CommandNamespaces?

    private val yamlData: MutableMap<String, Any>
    private val dispCmdName: String?
    val name: String
    private val validateOnly: Boolean
    var variablesResolved = false

    val isEmpty: Boolean
        get() = yamlData[name] == null

    // Get whole thing
    val string: String
        get() {
            val obj = yamlData[name] ?: return ""
            if (obj is Map<*, *>) {
                throw RuntimeException("Cannot convert map-based properties into a single string")
            }
            return requireNotNull(obj.toString())
        }

    val strings: List<String>
        get() {
            val obj = yamlData[name]
            if (obj is List<*>) {
                return obj.map{ it.toString() }
            } else if (obj is Map<*, *>) {
                throw RuntimeException("Unable to parse list of Strings from Map")
            }

            val str = obj.toString()
            return if (!str.isEmpty()) {
                arrayListOf(str)
            } else emptyList()
        }

    // Purely for EMPTY / empty()
    private constructor() {
        this.ns = null
        this.yamlData = mutableMapOf()
        this.dispCmdName = null
        this.name = "__empty__"
        this.validateOnly = false
    }

    constructor(inCtxt: VariablesContext, inNS: CommandNamespaces, inCmdName: String, inValue: Any) {
        ns = inNS

        name = inCmdName
        dispCmdName = ns.toCanonicalName(name)

        validateOnly = false

        yamlData = hashMapOf()
        yamlData[inCmdName] = inValue

        resolveAllVariables(yamlData as MutableMap<Any?,Any?>, inCtxt, false)
    }

    constructor(inCtxt: VariablesContext,
                inNS: CommandNamespaces,
                inData: Map<String, Any>,
                inValidateOnly: Boolean) {
        ns = inNS

        validateOnly = inValidateOnly

        // Should only be 1 (except for type:) but in any event, we want the *first* (order *will* be consistent!)
        name = inData.keys.first()
        dispCmdName = ns.toCanonicalName(name)

        yamlData = cloneIncomingCommandData(inData as Map<Any?, Any>)

        // Previously we'd skip resolution in 'validate' mode, but that was insane: (a) useless, as validation routines
        // didn't get to see the 'real' values, and (b) misleading all round.
        resolveAllVariables(yamlData as MutableMap<Any?,Any?>, inCtxt, inValidateOnly)
    }

    // Aggressively clone data to make absolutely sure our var-resolving doesn't tamper with the YAML doc structure
    private fun cloneIncomingCommandData(inData: Map<Any?, Any>): MutableMap<String, Any> {
        return IOUtils.deepCloneMap(inData)
    }

    private fun resolveAllVariables(ioData: MutableMap<Any?,Any?>,
                                    inCtxt: VariablesContext,
                                    inValidateOnly: Boolean) {

        // First pass, to resolve key names
        for (eachKeyObj in /* Prevent CME */ ArrayList(ioData.keys)) {
            val eachKeyStr = eachKeyObj.toString()
            val resolvedKeyName = inCtxt.resolve(eachKeyStr as String?, /* Never strict */ CLEAR_IF_MISSING, inValidateOnly)
            if (eachKeyStr != resolvedKeyName) {  // Needs changing?
                ioData[resolvedKeyName] = ioData.remove(eachKeyStr)
                variablesResolved = true
            }
        }

        for ((key, value) in ioData) {
            if (value is String) {
                ioData[ inCtxt.resolve(key!!) ] = inCtxt.resolve(value as String?, if (validateOnly) CLEAR_IF_MISSING else FAIL_IF_MISSING, inValidateOnly)!!
                variablesResolved = true
            } else if (value is Map<*, *>) {
                resolveAllVariables(value as MutableMap<Any?, Any?>, inCtxt, inValidateOnly)
            } else if (value is List<*>) {
                if (value.isEmpty()) {
                    continue
                }

                // Make sure we *actually* have a Map!
                val obj = value.first()

                if (obj is Map<*, *>) {
                    for (eachMap in ConfigUtils.mapElements(value)) {
                        resolveAllVariables(eachMap as MutableMap<Any?, Any?>, inCtxt, inValidateOnly)
                    }
                } else if (obj is List<*>) {
                    throw UnsupportedOperationException("List<List> not supported!")
                } else {
                    // Yuk, have to modify the List in-place
                    val size = value.size
                    for (idx in 0 until size) {
                        val eachStrValue = value[idx].toString()
                        val resolvedKeyName = inCtxt.resolve(eachStrValue as String?, /* Never strict */ CLEAR_IF_MISSING, inValidateOnly)
                        if (eachStrValue != resolvedKeyName) {  // Needs changing?
                            (value as MutableList<String?>)[idx] = resolvedKeyName
                        }
                    }
                }
            }
        }
    }

    fun asMap(): Map<String, Any> {
        return yamlData[name] as Map<String, Any>
    }

    fun propertyNames(): Set<String> {
        return asMap().keys
    }

    // Strictly speaking it's possible to pass non-String keys (by mistake!?)
    // We filter them out at this point.
    private fun entryForName(ioData: Map<Any, Any?>, inName: String): Entry<String, Any?>? {
        for (each in ioData.entries.filter { it.key is String }) {
            if (each.key == inName) {
                return each as Entry<String, Any?>
            }
            if (each.value is Map<*, *>) {
                val e = entryForName(each.value as Map<Any, Any?>, inName)
                if (e != null) {
                    return e
                }
            }
        }
        return null
    }

    fun has(propName: String): Boolean {
        if (name == propName) {
            throw RuntimeException("Should not call `has(<command name>)`, which is likely a mistake")
        }
        return entryForName(yamlData as Map<Any,Any?>, propName) != null
    }

    fun missing(propName: String): Boolean {
        if (name == propName) {
            throw RuntimeException("Should not call `missing(<command name>)`, which is likely a mistake")
        }
        return entryForName(yamlData as Map<Any,Any?>, propName) == null
    }

    fun isNamed(propName: String): Boolean {
        return name == propName
    }

    fun hasString(): Boolean {
        val obj = yamlData[name]
        return if (obj is String) {
            !obj.isEmpty()
        } else false
    }

    // Get whole thing, if available
    fun optString(): Optional<String> {
        val obj = yamlData[name]
        return if (obj !is String) {
            Optional.empty()
        } else Optional.of(obj)
    }

    // Get whole thing, if available
    fun optInteger(): Optional<Int> {
        val obj = yamlData[name] ?: return Optional.empty()
        return if (obj is Number) {
            Optional.of(obj.toInt())
        } else Optional.of(Integer.parseInt(obj as String))
    }

    fun optDuration(): Optional<Duration> {
        val os = optString()
        return if (!os.isPresent) {
            Optional.empty()
        } else parseDurationString(os.get())

    }

    private fun parseDurationString(s: String): Optional<Duration> {
        return try {
            Optional.of(ParseUtils.parseDuration(s))
        } catch (e: RuntimeException) {
            Optional.empty()
        }

    }

    private fun entryForPropertyName(propName: String): Entry<String?, Any?>? {
        if (name == propName) {
            // Skip top level, start at Map child
            val body = yamlData[name]
            return if (body is Map<*, *>) entryForName(body as Map<Any,Any?>, propName) else null
        }
        return entryForName(yamlData as Map<Any,Any?>, propName)
    }

    fun getString(propName: String): String {
        val e = entryForPropertyName(propName) ?: throw RuntimeException("No string property named '$propName'")
        return e.value.toString()
    }

    fun optString(propName: String): Optional<String> {
        val e = entryForPropertyName(propName)
        if (e?.value == null) {
            return Optional.empty()
        }

        val s = e.value.toString()
        return if (s.isEmpty()) {
            Optional.empty()
        } else Optional.of(s)

    }

    fun optFile(propName: String): Optional<File> {
        val os = optString(propName)
        if (!os.isPresent) {
            return Optional.empty()
        }

        val f = File(FileUtils.resolveTildes(os.get()))
        require(f.exists()) {"File doesn't exist: $f"}
        return Optional.of(f)
    }

    fun getDuration(propName: String): Duration {
        return ParseUtils.parseDuration(getString(propName))
    }

    fun optDuration(propName: String): Optional<Duration> {
        val os = optString(propName)
        return if (!os.isPresent) {
            Optional.empty()
        } else parseDurationString(os.get())

    }

    fun getMap(propName: String): Map<String, Any> {
        val e = entryForPropertyName(propName) ?: throw RuntimeException("No map-based property named '$propName'")
        return e.value as Map<String, Any>
    }

    fun getList(propName: String): List<Map<String, Any>> {
        val e = entryForPropertyName(propName) ?: throw RuntimeException("No list-based property named '$propName'")
        return e.value as List<Map<String, Any>>
    }

    fun optList(propName: String): Optional<List<Map<String, Any>>> {
        val e = entryForPropertyName(propName)
        return if (e?.value == null) {
            Optional.empty()
        } else Optional.of(e.value as List<Map<String, Any>>)
    }

    fun getStrings(propName: String): List<String> {
        val e = entryForPropertyName(propName) ?: throw RuntimeException("No strings property named '$propName'")

        if (e.value is Map<*, *>) {
            val urls = arrayListOf<String>()
            for (each in (e.value as Map<String, Any>).keys) {
                if (each.isEmpty()) {
                    continue
                }
                urls.add(each)
            }
            return urls
        }

        if (e.value is List<*>) {
            return (e.value as List<*>).map{ it.toString() }
        }

        val str = e.value as String?
        return if (str.isNullOrEmpty()) {
            emptyList()
        } else listOf(str!!)

    }

    fun getInteger(propName: String): Int {
        val e = entryForPropertyName(propName) ?: throw RuntimeException("No integer property named '$propName'")
        return Integer.parseInt(e.value.toString())
    }

    fun getBoolean(propName: String): Boolean {
        val e = entryForPropertyName(propName) ?: throw RuntimeException("No boolean property named '$propName'")
        return java.lang.Boolean.parseBoolean(e.value.toString())
    }

    fun optBoolean(propName: String): Optional<Boolean> {
        val e = entryForPropertyName(propName)
        if (e?.value == null) {
            return Optional.empty()
        }

        return if (e.value is Boolean) {
            Optional.of(e.value as Boolean)
        } else Optional.of(java.lang.Boolean.parseBoolean(e.value.toString()))

    }

    fun optInteger(propName: String): Optional<Int> {
        val e = entryForPropertyName(propName)
        if (e?.value == null) {
            return Optional.empty()
        }

        return if (e.value is Number) {
            Optional.of((e.value as Number).toInt())
        } else Optional.of(Integer.parseInt(e.value.toString()))

    }

    fun optLong(propName: String): Optional<Long> {
        val e = entryForPropertyName(propName)
        if (e?.value == null) {
            return Optional.empty()
        }

        return if (e.value is Number) {
            Optional.of((e.value as Number).toLong())
        } else Optional.of(java.lang.Long.parseLong(e.value.toString()))

    }

    fun childElements(propName: String): List<Map<String, Any>> {
        return requireNotNull(yamlData[propName] as List<Map<String, Any>>) {"No actual children within '$name:' statement"}
    }

    //	public List<CommandSpec> children(final String propName) {
    //		return FluentIterable.from( childElements(propName) ).transform( new Function<Map<String,Object>,CommandSpec>() {
    //
    //			@Override
    //			public CommandSpec apply( Map<String, Object> input) {
    //				return new CommandSpec( varCtxt, ns, input, validateOnly);
    //			}} ).toList();
    //	}

    private fun removeEntryForName(ioData: MutableMap<Any,Any>, inName: String): Boolean {
        var foundKey = false
        for ((key, value) in ioData.filter { it.key is String }) {
            if (key == inName) {
                foundKey = true
                break
            }
            if (value is Map<*, *>) {
                if (removeEntryForName(value as MutableMap<Any,Any>, inName)) {
                    return true
                }
            }
        }
        if (foundKey) {
            ioData.remove(inName)
        }
        return foundKey
    }

    // FIXME *Should* just hide the property from future queries. But simpler to just drop it...
    fun disable(propName: String) {
        removeEntryForName(yamlData as MutableMap<Any,Any>, propName)
    }

    override fun hashCode(): Int {
        return yamlData.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (other !is CommandSpec) {
            return false
        }
        val otherCmd = other as CommandSpec?
        return yamlData == otherCmd!!.yamlData
    }

    override fun toString(): String {
        return if (yamlData.size == 1) {
            @Suppress("USELESS_ELVIS")
            "CommandSpec('" + dispCmdName + "'):" + (yamlData[name] ?: "<empty>")
        } else "CommandSpec('$dispCmdName'):$yamlData"
    }

    companion object {

        private val EMPTY = CommandSpec()

        fun empty(): CommandSpec {
            return EMPTY
        }
    }
}
