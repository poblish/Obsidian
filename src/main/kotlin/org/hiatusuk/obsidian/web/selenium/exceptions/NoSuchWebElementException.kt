package org.hiatusuk.obsidian.web.selenium.exceptions

/**
 * A less noisy replacement for Selenium's org.openqa.selenium.NoSuchElementException
 *
 */
class NoSuchWebElementException(reason: String) : RuntimeException(reason)