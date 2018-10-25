package org.hiatusuk.obsidian.web.selenium.exceptions

/**
 * A less noisy replacement for Selenium's org.openqa.selenium.NoSuchWebElementException
 *
 */
class WebElementNotVisibleException(reason: String) : RuntimeException(reason)