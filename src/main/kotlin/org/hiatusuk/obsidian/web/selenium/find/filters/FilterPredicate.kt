package org.hiatusuk.obsidian.web.selenium.find.filters

import org.openqa.selenium.WebElement

class FilterPredicate(private val filter: Filter?) : (WebElement?) -> Boolean {

    override fun invoke(elem: WebElement?): Boolean {
        return filter != null && filter.matches(returnTextIfTextAttributeElseAttributeValue(elem))
    }

    private fun returnTextIfTextAttributeElseAttributeValue(elem: WebElement?): String {
        return if ("text".equals(filter!!.attr(), ignoreCase = true)) elem!!.text else elem!!.getAttribute(filter.attr())
    }
}