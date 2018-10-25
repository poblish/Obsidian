package org.hiatusuk.obsidian.gui

import com.google.common.annotations.VisibleForTesting
import org.hiatusuk.obsidian.run.LocalBrowserProperties
import org.hiatusuk.obsidian.run.LocalBrowserProperties.Companion.CHROME_DRIVER_PATH
import org.hiatusuk.obsidian.run.LocalBrowserProperties.Companion.IE_DRIVER_PATH
import org.hiatusuk.obsidian.run.LocalBrowserProperties.Companion.OPERA_DRIVER_PATH
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.swing.*

class BrowserOptionsFrame(ownerFrame: JFrame,
                          localBrowsers: LocalBrowserProperties) : JDialog(ownerFrame, "Local Browser Options", Dialog.ModalityType.APPLICATION_MODAL) {

    init {
        this.layout = GridBagLayout()

        val gBC = GridBagConstraints()

        var rowNum = 0

        val chromeLabel = JLabel("Chrome executable location:")
        gBC.gridx = 0
        gBC.gridy = rowNum++
        gBC.weightx = 0.0
        gBC.gridwidth = 3
        gBC.anchor = GridBagConstraints.WEST
        gBC.insets = Insets(TOP_MARGIN, LEFT_MARGIN, 0, RIGHT_MARGIN)
        this.add(chromeLabel, gBC)

        val chromeFld = JTextField(localBrowsers.currentChromeDriverPath())
        chromeFld.minimumSize = Dimension(250, 20)
        chromeFld.preferredSize = Dimension(430, 20)
        gBC.gridx = 0
        gBC.gridy = rowNum++
        gBC.weightx = 0.0
        gBC.insets = Insets(5, LEFT_MARGIN, 0, RIGHT_MARGIN)
        this.add(chromeFld, gBC)

        val operaLabel = JLabel("Opera executable location:")
        gBC.gridx = 0
        gBC.gridy = rowNum++
        gBC.weightx = 0.0
        gBC.gridwidth = 3
        gBC.anchor = GridBagConstraints.WEST
        gBC.insets = Insets(TOP_MARGIN, LEFT_MARGIN, 0, RIGHT_MARGIN)
        this.add(operaLabel, gBC)

        val operaFld = JTextField(localBrowsers.currentOperaDriverPath())
        operaFld.minimumSize = Dimension(250, 20)
        operaFld.preferredSize = Dimension(430, 20)
        gBC.gridx = 0
        gBC.gridy = rowNum++
        gBC.weightx = 0.0
        gBC.insets = Insets(5, LEFT_MARGIN, 0, RIGHT_MARGIN)
        this.add(operaFld, gBC)

        val ieLabel = JLabel("IE executable location:")
        gBC.gridx = 0
        gBC.gridy = rowNum++
        gBC.weightx = 0.0
        gBC.insets = Insets(12, LEFT_MARGIN, 0, RIGHT_MARGIN)
        this.add(ieLabel, gBC)

        val ieFld = JTextField(localBrowsers.currentIeDriverPath())
        ieFld.minimumSize = Dimension(250, 20)
        ieFld.preferredSize = Dimension(430, 20)
        gBC.gridx = 0
        gBC.gridy = rowNum++
        gBC.weightx = 0.0
        gBC.insets = Insets(5, LEFT_MARGIN, 0, RIGHT_MARGIN)
        this.add(ieFld, gBC)

        val ffLabel = JLabel("<html>Firefox is auto-detected; Safari requires plug-in as<br>per: <a href=\"$SAFARI_URL\">$SAFARI_URL</a></html>")
        gBC.gridx = 0
        gBC.gridy = rowNum++
        gBC.weightx = 0.0
        gBC.insets = Insets(12, LEFT_MARGIN, 0, RIGHT_MARGIN)
        this.add(ffLabel, gBC)

        ffLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    Desktop.getDesktop().browse(URI(SAFARI_URL))
                } catch (ex: URISyntaxException) {
                    //It looks like there's a problem
                } catch (ex: IOException) {
                }

            }
        })

        val cancelButton = JButton("Cancel")
        gBC.gridx = 1
        gBC.gridy = rowNum
        gBC.weightx = 0.0
        gBC.gridwidth = 1
        gBC.anchor = GridBagConstraints.EAST
        gBC.insets = Insets(16, 0, BOTTOM_MARGIN, 8)
        this.add(cancelButton, gBC)

        cancelButton.addActionListener { this@BrowserOptionsFrame.isVisible = false }

        val okButton = JButton("OK")
        gBC.gridx = 2
        gBC.gridy = rowNum
        gBC.weightx = 0.0
        gBC.gridwidth = 1
        gBC.anchor = GridBagConstraints.EAST
        gBC.insets = Insets(16, 0, BOTTOM_MARGIN, RIGHT_MARGIN)
        this.add(okButton, gBC)

        okButton.addActionListener {
            localBrowsers.updateConfiguration(CHROME_DRIVER_PATH, chromeFld.text)
            localBrowsers.updateConfiguration(OPERA_DRIVER_PATH, operaFld.text)
            localBrowsers.updateConfiguration(IE_DRIVER_PATH, ieFld.text)

            this@BrowserOptionsFrame.isVisible = false
        }

        this.getRootPane().defaultButton = okButton

        this.pack()
        setLocationRelativeTo(ownerFrame)

        CURRENT_INSTANCE = this
    }

    companion object {

        private const val serialVersionUID = 1L

        private const val SAFARI_URL = "https://code.google.com/p/selenium/wiki/SafariDriver"

        private const val TOP_MARGIN = 12
        private const val LEFT_MARGIN = 12
        private const val RIGHT_MARGIN = 12
        private const val BOTTOM_MARGIN = 12

        @VisibleForTesting
        internal var CURRENT_INSTANCE: BrowserOptionsFrame? = null
    }
}
