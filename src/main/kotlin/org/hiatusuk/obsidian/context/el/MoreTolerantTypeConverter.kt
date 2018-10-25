package org.hiatusuk.obsidian.context.el

import org.hiatusuk.obsidian.utils.ParseUtils
import org.hiatusuk.obsidian.utils.StringUtils

import de.odysseus.el.misc.TypeConverterImpl

/**
 * An implementation that supports commas within numbers.
 *
 */
class MoreTolerantTypeConverter : TypeConverterImpl() {

    override fun coerceToType(value: Any?, type: Class<*>): Any {
        if (value is String) {
            // If, stripped of a comma, we become parseable as a number, strip the comma. Change nothing else!
            val commaStrippedStr = StringUtils.replace(value, ",", "")
            if (ParseUtils.parseAsDouble(commaStrippedStr) != null) {
                return super.coerceToType(commaStrippedStr, type)
            }
        }

        return super.coerceToType(value, type)
    }
}
