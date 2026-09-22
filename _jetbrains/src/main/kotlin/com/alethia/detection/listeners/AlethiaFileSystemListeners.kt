package com.alethia.detection.listeners

import com.alethia.detection.AlethiaEventHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.slf4j.LoggerFactory

/**
 * Detects file renames in the project.
 * A renamed file may break import paths or dependencies
 * in other parts of the codebase.
 */
class AlethiaFileRenameListener(private val project: Project) : BulkFileListener {

    private val LOG = LoggerFactory.getLogger(AlethiaFileRenameListener::class.java)
    private val handler = project.service<AlethiaEventHandler>()

    /**
     * Fires after Virtual File Events.
     * Detects a change in file names, submitting a
     * DetectionEvent to AlethiaEventHandler.
     */
    override fun after(events: List<VFileEvent>) {

    }
}

/**
 * Detects directory renames in the project.
 * A renamed directory may break package declarations,
 * import paths, and build configuration references.
 */
class AlethiaDirectoryRenameListener(private val project: Project) : BulkFileListener {

    private val LOG = LoggerFactory.getLogger(AlethiaDirectoryRenameListener::class.java)
    private val handler = project.service<AlethiaEventHandler>()

    /**
     * Fires after Virtual File Events.
     * Detects a change in directory names, submiting a
     * DetectionEvent to AlethiaEventHandler.
     */
    override fun after(events: List<VFileEvent>) {

    }
}