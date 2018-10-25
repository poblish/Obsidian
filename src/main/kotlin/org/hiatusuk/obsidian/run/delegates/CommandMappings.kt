package org.hiatusuk.obsidian.run.delegates

import com.codahale.metrics.MetricRegistry
import com.google.common.base.Throwables
import com.google.common.eventbus.EventBus
import org.hiatusuk.obsidian.cmd.api.Command
import org.hiatusuk.obsidian.cmd.api.CommandIF
import org.hiatusuk.obsidian.cmd.api.CommandSpec
import org.hiatusuk.obsidian.cmd.api.Validate
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.di.includes.CommandsComponent
import org.hiatusuk.obsidian.run.FailFastMode
import org.hiatusuk.obsidian.run.RunProperties
import org.hiatusuk.obsidian.run.events.ScenarioStartup
import org.hiatusuk.obsidian.run.exceptions.LifecycleMethodException
import org.hiatusuk.obsidian.run.exceptions.TerminationException
import org.slf4j.Logger
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@ScenarioScope
class CommandMappings @Inject
internal constructor(private val commands: CommandsComponent,
                     private val ns: CommandNamespaces,
                     private val runProps: RunProperties,
                     private val eventBus: EventBus,
                     private val reflections: ReflectionsState,
                     private val metrics: MetricRegistry,
                     private val log: Logger) {

    private val commandMethodsCache: Array<Method> = commands.javaClass.methods.filter {
        if (it.returnType !== Void.TYPE &&
            it.returnType.getAnnotation(Command::class.java) != null &&
            it.returnType.getAnnotation(Singleton::class.java) == null &&
            it.returnType.getAnnotation(ScenarioScope::class.java) == null)
        {
            throw java.lang.RuntimeException("All @Command's should be @Singleton or @ScenarioScope... " + it.returnType)
        }
        true
        }.toTypedArray()

    private val mappings = HashMap<Any, Class<CommandIF>>()
    private var done: Boolean = false

    @ScenarioStartup
    fun startUp() {
        if (done) {
            return
        }

        val ref = reflections.all()  // Outside the timer...

        metrics.timer("CmdMappings.generate").time().use {
            for (eachClass in ref.getTypesAnnotatedWith(Command::class.java)) {
                val cmd = eachClass.getAnnotation(Command::class.java)

                if (mappings.put(if (cmd.regex) Pattern.compile("^" + cmd.value) else cmd.value, eachClass as Class<CommandIF>) != null) {
                    throw RuntimeException("Duplicate mapping for '" + cmd.value + "'. '" + eachClass + "' cannot use it too.")
                }
            }

            log.trace("Derived: {}", mappings)
        }

        done = true
    }

    fun commandForName(inCmd: CommandSpec): CommandIF? {
        metrics.timer("CmdMappings.forName").time().use {
            val clz = commandClassForName(inCmd.name) ?: return null

            // System.out.println("**** " + clz + " for " + name);

            for (eachMethod in commandMethodsCache) {
                if (eachMethod.returnType == clz) {
                    eachMethod.isAccessible = true
                    return eachMethod.invoke(commands) as CommandIF
                }
            }

            return null
        }
    }

    fun validate(inCmd: CommandSpec) {
        metrics.timer("CmdMappings.validate").time().use {
            val clz = commandClassForName(inCmd.name)
                    ?: // log.debug("Can't validate... {}", clz);
                    return

            log.trace("Validate... {}", clz)

            var registeredInst: Any? = null

            for (eachInstanceMethod in clz.methods) {
                if (eachInstanceMethod.isAnnotationPresent(Validate::class.java)) {  // This method needs validating (*might* be multiple per class). Find instance...
                    try {
                        if (registeredInst == null) {
                            registeredInst = findInstanceOfClassAmongComponents(clz)
                        }

                        eachInstanceMethod.invoke(checkNotNull(registeredInst) {"Failed to obtain instance of '$clz'"}, inCmd)
                    } catch (originalT: Throwable) {
                        var target = originalT

                        if (originalT is InvocationTargetException) {
                            target = originalT.cause!!
                        }

                        if (target is TerminationException) {
                            throw target
                        } else if (runProps.failFastMode == FailFastMode.FAIL_ON_FIRST_MISMATCH) {
                            if (target is RuntimeException) {
                                throw target
                            }
                            Throwables.throwIfUnchecked(target)
                            throw RuntimeException(target)
                        } else {
                            // Pass it on to SuppressedErrors...
                            eventBus.post(LifecycleMethodException(target))
                        }
                    } finally {
                        log.trace("For @Validate, called: {}", eachInstanceMethod)
                    }
                }
            }
        }
    }

    private fun commandClassForName(rawName: String): Class<*>? {
        val resolvedName = ns.toCanonicalName(rawName)

        var clz: Class<*>? = null

        for ((key, value) in mappings) {
            // System.out.println("--- Try: " + each.getKey() + " against '" + targetId + "'");

            if (key is String && key == resolvedName) {
                clz = value
                break
            }

            if (key is Pattern && key.matcher(resolvedName).find()) {
                clz = value
                break
            }
        }

        return clz
    }

    @Throws(ReflectiveOperationException::class)
    private fun findInstanceOfClassAmongComponents(inClass: Class<*>): Any? {
        for (eachRegisteredMethod in commandMethodsCache) {
            if (inClass == eachRegisteredMethod.returnType) {

                if (!eachRegisteredMethod.isAccessible) {
                    eachRegisteredMethod.isAccessible = true
                }

                return eachRegisteredMethod.invoke(commands)  // Get instance from @Component
            }
        }
        return null
    }
}
