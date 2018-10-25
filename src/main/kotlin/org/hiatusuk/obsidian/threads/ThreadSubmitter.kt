package org.hiatusuk.obsidian.threads

import java.util.concurrent.Future

interface ThreadSubmitter {
    fun submitThread(task: () -> Unit): Future<*>
}
