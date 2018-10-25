package org.hiatusuk.obsidian.protocol.http.delegates

import com.google.common.annotations.VisibleForTesting
import org.hiatusuk.obsidian.di.ScenarioScope
import javax.inject.Inject

@ScenarioScope
class CurrentHttpRequestTimings @VisibleForTesting
@Inject constructor() {

    var lastExecTimeNs: Long = -1
    var lastSendTimeNs: Long = -1
    var lastReceiveTimeNs: Long = -1

    fun clearAll() {
        lastReceiveTimeNs = -1
        lastSendTimeNs = lastReceiveTimeNs
        lastExecTimeNs = lastSendTimeNs
    }

    fun setLastExecutionTime(l: Long) {
        lastExecTimeNs = l
    }

    fun setLastSendTime(l: Long) {
        lastSendTimeNs = l
    }

    fun setLastReceiveTime(l: Long) {
        lastReceiveTimeNs = l
    }
}