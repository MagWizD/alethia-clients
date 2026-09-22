package com.alethia.detection.listeners

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import org.slf4j.LoggerFactory


/**
 * Tracks when the IDE regains focus after losing it.
 * A large paste shortly after focus restore suggests code
 * was sourced from an external tool...
 */
class AlethiaFocusListener : ApplicationActivationListener {

    private val LOG = LoggerFactory.getLogger(AlethiaFocusListener::class.java)

    /**
     *  Records the time when the IDE regained focus, used
     *  to detemine whether a paste event is a LARGE_PASTE
     *  or FOCUS_RESTORE_PASTE
     */
    override fun applicationActivated(ideFram: IdeFrame) {
        // Record focus restore gap and store for paste listener to check
        lastFocusRestoreTime = System.currentTimeMillis()
    }

    companion object {
        var lastFocusRestoreTime: Long = 0
    }
}
