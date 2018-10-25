package org.hiatusuk.obsidian.run

import com.google.common.base.Throwables
import com.google.common.eventbus.EventBus
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.di.includes.EventsComponent
import org.hiatusuk.obsidian.run.events.AllEvents
import org.hiatusuk.obsidian.run.exceptions.LifecycleMethodException
import org.hiatusuk.obsidian.run.exceptions.TerminationException
import org.slf4j.LoggerFactory
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.inject.Inject

@ScenarioScope
class Lifecycle @Inject
constructor(private val events: EventsComponent,
            private val runProps: RunProperties,
            private val eventBus: EventBus) {

    // Cache all eligible methods, currently anything that doesn't come from Object itself
    private val eventsMethodsCache: List<Method> = events.javaClass.methods.filter { !it.declaringClass.name.contains("java.lang.Object") }

    fun call(inAnnotation: Class<out Annotation>) {
        callLifecycleMethodWithHandler(inAnnotation) { registeredInst, eachInstanceMethod, actualEventType ->

            // If we have an annotation param, that means we're looking at a *particular* one via @AllEvents
            if (actualEventType != null) {
                eachInstanceMethod.invoke(registeredInst, actualEventType)
            } else {
                eachInstanceMethod.invoke(registeredInst) // Call no-arg instance method
            }
        }
    }

    fun call(inAnnotation: Class<out Annotation>, inArg: Any) {
        callLifecycleMethodWithHandler(inAnnotation) { registeredInst, eachInstanceMethod, actualEventType ->

            // If we have an annotation param, that means we're looking at a *particular* one via @AllEvents
            when {
                actualEventType != null -> eachInstanceMethod.invoke(registeredInst, actualEventType)
                eachInstanceMethod.parameterTypes.isEmpty() -> eachInstanceMethod.invoke(registeredInst) // Method doesn't actually accept args, so don't send our one.
                else -> eachInstanceMethod.invoke(registeredInst, inArg) // Call 1-arg instance method. Allow it to fail hard for > 1 args
            }
        }
    }

    private fun callLifecycleMethodWithHandler(inAnnotation: Class<out Annotation>,
                                               inCaller: (registeredInst: Any, instanceMethod: Method, eventType: Class<out Annotation>?) -> Unit) {
        for (eachRegisteredMethod in eventsMethodsCache) {
            var found = false

            for (eachInstanceMethod in eachRegisteredMethod.returnType.methods) {

                val acceptsAllEvents = eachInstanceMethod.isAnnotationPresent(AllEvents::class.java)

                if (acceptsAllEvents || eachInstanceMethod.isAnnotationPresent(inAnnotation)) {

                    if (!eachRegisteredMethod.isAccessible) {
                        eachRegisteredMethod.isAccessible = true
                    }

                    val registeredInst = eachRegisteredMethod.invoke(this.events)  // Get instance from @Component
                    if (registeredInst == null) {  // Almost certainly only for mocks / tests
                        LOG.warn("Ignoring @{} lifecycle event for: {}", inAnnotation.simpleName, eachRegisteredMethod)
                        return
                    }

                    found = true  // Don't break. Might as well support multiple methods per class. Should set *before* call

                    try {
                        inCaller(registeredInst, eachInstanceMethod, if (acceptsAllEvents) inAnnotation else null)
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
                        LOG.trace("For @$inAnnotation, called: {}", eachInstanceMethod)
                    }
                }
            }

            if (!found && LOG.isTraceEnabled) {
                LOG.trace("No @{} handler in: {}", inAnnotation.simpleName, eachRegisteredMethod.returnType)
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Lifecycle::class.java)
    }
}
