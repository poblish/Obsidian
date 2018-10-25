package org.hiatusuk.obsidian.gui

import com.google.common.annotations.VisibleForTesting
import org.hiatusuk.obsidian.Obsidian
import org.hiatusuk.obsidian.run.GuiRunRequest
import org.hiatusuk.obsidian.run.LocalBrowserProperties
import org.hiatusuk.obsidian.run.RunProperties
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.Optional
import java.util.prefs.Preferences
import javax.swing.*
import javax.swing.filechooser.FileFilter
import kotlin.collections.ArrayList

class GuiDelegate {

    private val listeners = ArrayList<(GuiRunRequest) -> Unit>()

    @VisibleForTesting
    internal var localBrowserOptsButton: JButton? = null

    @VisibleForTesting
    var runButton: JButton? = null

    fun showGUI(props: RunProperties, localBrowsers: LocalBrowserProperties): JFrame? {
        try {
            val prefs = Preferences.userNodeForPackage(Obsidian::class.java)

            val jf = JFrame("Obsidian")

            jf.addWindowListener(object : WindowAdapter() {

                override fun windowClosing(e: WindowEvent?) {
                    System.exit(0)  // FIXME!
                }
            })

            jf.layout = GridBagLayout()
            val gBC = GridBagConstraints()

            ////////////////////////////////////////////////////////

            val filesPanel = JPanel()

            val pathLabel = JLabel("Scenario path(s):")
            gBC.gridx = 0
            gBC.gridy = 0
            gBC.weightx = 0.0
            gBC.gridwidth = 1
            gBC.insets = Insets(16, 16, 16, 8)
            filesPanel.add(pathLabel, gBC)

            val pathFld = JTextArea(prefs.get("initial.path", ""))
            pathFld.wrapStyleWord = true
            pathFld.lineWrap = true

            val pathScroll = JScrollPane(pathFld)

            pathScroll.minimumSize = Dimension(250, 20)
            pathScroll.preferredSize = Dimension(430, 80)
            gBC.gridx = 1
            gBC.gridy = 0
            gBC.weightx = 1.0
            gBC.gridwidth = 2
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(16, 0, 0, 16)
            filesPanel.add(pathScroll, gBC)

            val chooseButton = createChooseFileButton(jf, pathFld, prefs)

            gBC.gridx = 0
            gBC.gridy = 6
            gBC.weightx = 0.0
            gBC.gridwidth = 1
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(0, 0, 16, 16)
            filesPanel.add(chooseButton, gBC)

            ////////////////////////////////////////////////////////

            val scratchPanel = JPanel()

            val scratchTextFld = JTextArea(prefs.get("scratchPadContents", DEFAULT_SCRATCHPAD))
            scratchTextFld.wrapStyleWord = true
            scratchTextFld.lineWrap = true

            val scratchScroll = JScrollPane(scratchTextFld)

            scratchScroll.minimumSize = Dimension(250, 20)
            scratchScroll.preferredSize = Dimension(560, 100)

            gBC.gridx = 1
            gBC.gridy = 0
            gBC.weightx = 1.0
            gBC.gridwidth = 2
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(16, 0, 0, 16)
            scratchPanel.add(scratchScroll, gBC)

            ////////////////////////////////////////////////////////

            val restoredSelection = prefs.get("tabSelection", "files")

            val tabbedPane = JTabbedPane()
            tabbedPane.addTab("Scenario Files", filesPanel)
            tabbedPane.addTab("Scratchpad", scratchPanel)

            gBC.gridx = 1
            gBC.gridy = 0
            gBC.weightx = 1.0
            gBC.gridwidth = 2
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(8, LEFT_MARGIN, 0, RIGHT_MARGIN)
            jf.add(tabbedPane, gBC)

            tabbedPane.selectedIndex = if (restoredSelection == "files") 0 else 1

            tabbedPane.addChangeListener { e ->
                val tabSource = e.source as JTabbedPane
                when (tabSource.selectedIndex) {
                    0 -> prefs.put("tabSelection", "files")
                    1 -> prefs.put("tabSelection", "scratch")
                    else -> throw RuntimeException("Unknown")
                }
            }

            ////////////////////////////////////////////////////////

            val failFastCheck = JCheckBox("Exit test run on first failure")
            failFastCheck.isSelected = prefs.getBoolean("failfast", false)
            gBC.gridx = 2
            gBC.gridy = 1
            gBC.weightx = 0.0
            gBC.gridwidth = 1
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(0, LEFT_MARGIN, 0, 0)
            jf.add(failFastCheck, gBC)

            failFastCheck.addActionListener { prefs.putBoolean("failfast", failFastCheck.isSelected) }

            val logAssertionsCheck = JCheckBox("Log all assertions to console")
            logAssertionsCheck.isSelected = prefs.getBoolean("logAssertions", false)
            gBC.gridx = 2
            gBC.gridy = 2
            gBC.weightx = 0.0
            gBC.gridwidth = 1
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(0, LEFT_MARGIN, 0, 0)
            jf.add(logAssertionsCheck, gBC)

            logAssertionsCheck.addActionListener { prefs.putBoolean("logAssertions", logAssertionsCheck.isSelected) }

            val logMetricsCheck = JCheckBox("Log metrics/timings to a file?")
            logMetricsCheck.isSelected = prefs.getBoolean("logMetrics", true)
            gBC.gridx = 2
            gBC.gridy = 3
            gBC.weightx = 0.0
            gBC.gridwidth = 1
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(0, LEFT_MARGIN, 0, 0)
            jf.add(logMetricsCheck, gBC)

            logMetricsCheck.addActionListener { prefs.putBoolean("logMetrics", logMetricsCheck.isSelected) }

            //////////////////////////////////////////// Selenium Grid stuff

            val useGrid = java.lang.Boolean.parseBoolean(prefs.get("selenium.grid.use", "false"))

            val selGridPane = JPanel()
            val selGridUseGrid = JRadioButton("Use Grid:")
            val selGridUrlFld = JTextField(if (props.seleniumGridUrl.isPresent) props.seleniumGridUrl.get().toString() else prefs.get("selenium.grid.url", ""))
            val selGridUseLocal = JRadioButton("Local browsers only")

            selGridUseGrid.isSelected = useGrid
            selGridUseLocal.isSelected = !useGrid

            selGridPane.add(selGridUseLocal)
            selGridPane.add(selGridUseGrid)
            selGridPane.add(selGridUrlFld)

            selGridPane.layout = FlowLayout(FlowLayout.LEADING, 0, 0)

            val selGridRG = ButtonGroup()
            selGridRG.add(selGridUseGrid)
            selGridRG.add(selGridUseLocal)

            gBC.gridx = 2
            gBC.gridy = 4
            gBC.weightx = 0.0
            gBC.gridwidth = 2
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(8, LEFT_MARGIN, 8, 0)
            jf.add(selGridPane, gBC)

            //////////////////////////////////////////// Selenium Grid stuff ENDS

            localBrowserOptsButton = JButton("Local Browsers...")
            gBC.gridx = 2
            gBC.gridy = 5
            gBC.weightx = 0.0
            gBC.gridwidth = 1
            gBC.anchor = GridBagConstraints.WEST
            gBC.insets = Insets(0, LEFT_MARGIN, BOTTOM_MARGIN, 0)
            jf.add(localBrowserOptsButton, gBC)

            localBrowserOptsButton!!.addActionListener { BrowserOptionsFrame(jf, localBrowsers).isVisible = true }

            runButton = JButton("Run Scenarios")
            gBC.gridx = 2
            gBC.gridy = 5
            gBC.weightx = 0.0
            gBC.gridwidth = 1
            gBC.anchor = GridBagConstraints.EAST
            gBC.insets = Insets(0, 0, BOTTOM_MARGIN, RIGHT_MARGIN)
            jf.add(runButton, gBC)

            runButton!!.addActionListener {
                val path = pathFld.text.trim { r -> r <= ' ' }

                val tabSelection = prefs.get("tabSelection", "files")
                val scratchPad = if (tabSelection == "scratch") Optional.of(scratchTextFld.text) else Optional.empty()

                // Persist Prefs...
                prefs.put("initial.path", path)
                prefs.put("scratchPadContents", scratchTextFld.text)

                // Store these properties whether valid URL or not
                prefs.put("selenium.grid.use", selGridUseGrid.isSelected.toString())
                prefs.put("selenium.grid.url", selGridUrlFld.text)

                var selGridUrl = Optional.empty<URL>()

                if (selGridUseGrid.isSelected) {
                    try {
                        selGridUrl = Optional.of(URL(selGridUrlFld.text))
                    } catch (mue: MalformedURLException) {
                        LOG.error("Invalid Grid URL", mue)
                    }
                }

                val req = GuiRunRequest(arrayListOf(File(path)), scratchPad, failFastCheck.isSelected,
                        logAssertionsCheck.isSelected, logMetricsCheck.isSelected,
                        selGridUseGrid.isSelected, selGridUrl)
                for (each in listeners) {
                    each.invoke(req)
                }
            }

            jf.rootPane.defaultButton = runButton

            jf.pack()
            jf.isLocationByPlatform = true
            jf.isVisible = true

            return jf
        } catch (e: HeadlessException) {
            e.printStackTrace() // Ignore
            return null
        }

    }

    private fun createChooseFileButton(inFrame: JFrame, pathFld: JTextArea, prefs: Preferences): JButton {
        val chooseButton = JButton("Choose...")

        chooseButton.addActionListener {
            val fc = JFileChooser()
            fc.isAcceptAllFileFilterUsed = false
            fc.fileSelectionMode = JFileChooser.FILES_ONLY
            fc.addChoosableFileFilter(object : FileFilter() {

                override fun accept(f: File): Boolean {
                    return f.name.endsWith(".yaml") || f.name.endsWith(".yml")
                }

                override fun getDescription(): String {
                    return "Obsidian Scenario"
                }
            })

            if (fc.showOpenDialog(inFrame) == JFileChooser.APPROVE_OPTION) {  //  "Open an Obsidian Scenario YAML file"
                val path = fc.selectedFile.absolutePath
                pathFld.text = path

                // Persist Prefs...
                prefs.put("initial.path", path)
            }
        }

        return chooseButton
    }

    fun addListener(listener: (GuiRunRequest) -> Unit) {
        listeners.add(listener)
    }

    companion object {

        private const val LEFT_MARGIN = 12
        private const val RIGHT_MARGIN = 12
        private const val BOTTOM_MARGIN = 12

        private val LOG = LoggerFactory.getLogger("GUI")

        private const val DEFAULT_SCRATCHPAD = "browsers: {chrome}\n" +
                "URL Test:\n" +
                "- url: https://www.wikipedia.org/\n" +
                "- waitFor: {that: \"#js-link-box-ja strong\", eq: 日本語}"
    }
}