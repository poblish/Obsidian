package org.hiatusuk.obsidian.web.server.jetty.delegates

import org.eclipse.jetty.annotations.AnnotationConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.webapp.Configuration
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration
import org.eclipse.jetty.webapp.WebAppContext
import org.hiatusuk.obsidian.di.ScenarioScope
import org.hiatusuk.obsidian.run.events.AfterScenario
import org.hiatusuk.obsidian.run.events.ApplicationQuit
import org.hiatusuk.obsidian.web.server.jetty.JettyServerConfig
import org.slf4j.LoggerFactory
import java.util.Optional
import javax.inject.Inject
import kotlin.collections.ArrayList

@ScenarioScope
class JettyServers @Inject constructor()
{
    private var configToUse: JettyServerConfig? = null  // A bit iffy
    private var httpServer: Server? = null  // FIXME Only one currently supported at once
    private var ctxts: WebContexts? = null  // FIXME Only one currently supported at once

    fun setNextConfig(inConfig: JettyServerConfig) {
        configToUse = checkNotNull(inConfig)
        LOG.info("Will use {} to instantiate next Jetty", configToUse)
    }

    @AfterScenario
    fun resetAfterScenario() {
        ctxts = null
    }

    @ApplicationQuit
    fun shutdownAll() {
        if (httpServer != null) {
            LOG.info("Stopping... {}", httpServer)
            httpServer!!.stop()
            httpServer = null
        }
    }

    fun deployTo(path: String,
                 contextPath: String,
                 isWarDeploy: Optional<Boolean>) {

        // FIXME Should really check configs are the same (e.g. port!) before blindly reusing the last one.

        if (ctxts == null) {
            ctxts = WebContexts()
        }

        if (httpServer == null) {
            val jettyPort = if (configToUse != null) configToUse!!.port else JettyServerConfig.DEFAULT_SERVER_PORT
            httpServer = Server(jettyPort)

            val classlist = Configuration.ClassList.setServerDefault(httpServer!!)
            classlist.addBefore(JettyWebXmlConfiguration::class.java.name, AnnotationConfiguration::class.java.name)

            if (configToUse != null) {
                for (eachDef in configToUse!!.beanDefs()) {
                    val clazz = Class.forName(eachDef["type"] as String)
                    val beanInst = clazz.newInstance()

                    val attrs = ArrayList(eachDef.keys)
                    attrs.remove("type")

                    for (eachAttr in attrs) {
                        val m = clazz.getMethod("set" + Character.toTitleCase(eachAttr[0]) + eachAttr.substring(1), String::class.java)
                        m.invoke(beanInst, eachDef[eachAttr])
                    }

                    LOG.info("Adding Bean: {}", beanInst)

                    httpServer!!.addBean(beanInst)
                }
            }

            // Only needs doing once, simply add new Handlers to the collection to deploy them.
            httpServer!!.handler = ctxts!!.getHandler()

            LOG.info("Starting @{}...", jettyPort)
            httpServer!!.start()
        }

        //////////////////////////////////////////////////////////////////  Already started, just add apps...

        if (isWarDeploy.isPresent && isWarDeploy.get() || !isWarDeploy.isPresent && path.endsWith(".war")) {

            val app = WebAppContext()
            app.contextPath = contextPath
            app.war = path

            ctxts!!.addContext(app)
        }
        else {  // A non-WAR resource...
            val resHandler = ResourceHandler()
            resHandler.resourceBase = path
            resHandler.isDirectoriesListed = true

            ctxts!!.addContext(contextPath, resHandler)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("Jetty")
    }
}
