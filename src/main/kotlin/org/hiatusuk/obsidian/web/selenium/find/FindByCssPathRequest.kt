package org.hiatusuk.obsidian.web.selenium.find

import com.codahale.metrics.MetricRegistry
import java.util.Optional
import java.util.regex.Pattern

import org.hiatusuk.obsidian.web.selenium.delegates.WebState
import org.hiatusuk.obsidian.web.selenium.find.filters.Filter
import org.hiatusuk.obsidian.run.exceptions.RuntimeExceptions
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.slf4j.LoggerFactory

class FindByCssPathRequest(inWeb: WebState,
                           exceptions: RuntimeExceptions,
                           inMetrics: MetricRegistry,
                           path: String) : FindRequest(inWeb, exceptions, inMetrics) {

    private val path: String = escape( requireNotNull(path).trim() )

    init {

        if (path == "*" || path.startsWith("html")) {
            // Reluctantly allow this. Was: throw new InvalidCssException("We do not accept '*' as a valid CSS selector");
        }
        else if (path.length > 250 ||         // Clearly a bodge
                 path.contains("<") ||
                 path.contains(";") ||
                 path.contains(" - ") ||
                !VALID_CSS.matcher(this.path).find()) {  // *Possible* CSS. Do need to improve regex!
            throw exceptions.invalidCss("<$path> is not a valid CSS selector")
        }
    }

    public override fun getAll(vararg filters: Filter): Collection<WebElement> {
        try {
            val cwe = FindRequest.filter(timedFindElements(web.driverOnValidPage, By.cssSelector(path), metrics), *filters)

            if (cwe.size == 1) {
                checkElementIdMatching(cwe.first())
            }

            return cwe
        } catch (e: org.openqa.selenium.InvalidSelectorException) {
            throw exceptions.invalidSelector("The web selector '$path' is either invalid or does not result in a WebElement")
        }
        catch (e : UnsupportedOperationException) {  // NullWebDriver getting called. Ensure we get a line number
            throw exceptions.unsupportedOperation()
        }
    }

    public override fun getOptionalFirst(vararg filters: Filter): Optional<WebElement> {
        val owe = FindRequest.filter(timedFindElement(web.driverOnValidPage, By.cssSelector(path), metrics), *filters)

        owe.ifPresent { this.checkElementIdMatching(it) }

        return owe
    }

    // Any element found *with* an id from a single-elem lookup but that wasn’t specified *solely* by Id => WARN
    // Bear in mind that it's *illegal* to duplicate Ids (Id-lookups returning > 1 match)
    private fun checkElementIdMatching(elem: WebElement) {
        if (!SINGLE_ID_DETECTOR.matcher(path).matches()) {
            val id = escape( elem.getAttribute("id") )
            if (!id.isEmpty()) {
                LOG.warn("Single element found via '{}', but could simply have been found via '#{}'", path, id)
            }
        }
    }

    override fun toString(): String {
        return "first element with CSS path '$path'"
    }

    companion object {

        // See: https://mathiasbynens.be/notes/css-escapes etc.
        private val CSS_ENCODE_PATTERN = Regex("([!|\"|#|\$|%|&|'|(|)|*|+|,|-|.|/|:|;|<|=|>|?|@|[|\\|]|^|`|{|\\||}|~])")

        internal fun escape(rawCss: String): String {
            // For each Id clause in the entire CSS, ensure each one is encoded right, and join the results back together
            return rawCss.split(' ').asSequence().map { encodeIdPaths(it) }.joinToString(" ")
        }

        // For an Id (#...) clause, prefix every encodable character with a backslash
        private fun encodeIdPaths(str: String): Any {
            if (str.startsWith("#")) {
                return "#" + CSS_ENCODE_PATTERN.replace(str.substring(1)) { "\\" + it.value }
            }
            return str
        }

        // Should represent all *possible* CSS. The sections:
        // 1) Match attrs on their own... [name="example-ngModel-getter-setter"]
        // 2) Support for '*'
        // 3) Match all past => HTML5 tags, plus some/all angular extensions
        private val VALID_CSS = Pattern.compile("\\[.*\\s*=\\s*['\"].*['\"]\\]|\\B(?:#\\b|\\.[-_A-Z0-9]|\\*[:\\[])|(?:\\b(a|abbr|acronym|address|applet|area|article|aside|audio|b|base|basefont|bdi|bdo|big|blockquote|body|br|button|canvas|caption|center|cite|code|col|colgroup|datalist|dd|del|details|dfn|dialog|dir|div|dl|dt|em|embed|fieldset|figcaption|figure|font|footer|form|frame|frameset|h[1-6]|head|header|hr|i|iframe|img|input|ins|kbd|keygen|label|legend|li|link|main|map|mark|menu|menuitem|meta|meter|nav|ng-model|noframes|noscript|object|ol|optgroup|option|output|p|param|pre|progress|q|rp|rt|ruby|s|samp|script|section|select|small|source|span|strike|strong|style|sub|summary|sup|table|tbody|td|textarea|tfoot|th|thead|time|title|tr|track|tt|u|ul|var|video|wbr)\\b)", Pattern.CASE_INSENSITIVE)

        // Detect a#x and #blah ... NOT... div, div > a#x, div#x a
        private val SINGLE_ID_DETECTOR = Pattern.compile("^([^\\s]+)?#([^\\s]+)$")

        private val LOG = LoggerFactory.getLogger("FindByCssPath")
    }
}
