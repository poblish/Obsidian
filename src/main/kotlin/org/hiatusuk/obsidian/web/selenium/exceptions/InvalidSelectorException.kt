package org.hiatusuk.obsidian.web.selenium.exceptions

/**
 * A less noisy replacement for Selenium's org.openqa.selenium.InvalidSelectorException
 *
 */
class InvalidSelectorException(reason: String) : RuntimeException(reason)