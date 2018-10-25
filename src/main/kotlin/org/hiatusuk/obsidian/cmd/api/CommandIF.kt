package org.hiatusuk.obsidian.cmd.api

import java.io.IOException

interface CommandIF {

    @Throws(IOException::class)
    fun run(inCmd: CommandSpec)
}
