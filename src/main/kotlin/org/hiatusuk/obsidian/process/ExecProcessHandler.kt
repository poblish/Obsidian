package org.hiatusuk.obsidian.process

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import java.nio.ByteBuffer

class ExecProcessHandler : NuAbstractProcessHandler() {

    private val outBuf = StringBuilder()
    private val errBuf = StringBuilder()

    val stdout: String
        get() = outBuf.toString()

    val stderr: String
        get() = errBuf.toString()

    override fun onStdout(buffer: ByteBuffer, closed: Boolean) {
        val bytes = ByteArray(buffer.remaining())
        // You must update buffer.position() before returning (either implicitly,
        // like this, or explicitly) to indicate how many bytes your handler has consumed.
        buffer.get(bytes)
        outBuf.append(String(bytes))
    }

    override fun onStderr(buffer: ByteBuffer, closed: Boolean) {
        val bytes = ByteArray(buffer.remaining())
        // You must update buffer.position() before returning (either implicitly,
        // like this, or explicitly) to indicate how many bytes your handler has consumed.
        buffer.get(bytes)
        errBuf.append(String(bytes))
    }
}