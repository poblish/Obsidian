package org.hiatusuk.obsidian.web.selenium.exceptions

/**
 * A less noisy replacement for Selenium's org.openqa.selenium.remote.UnreachableBrowserException
 *
 */
class UnreachableWebBrowserException(reason: String) : RuntimeException(reason)