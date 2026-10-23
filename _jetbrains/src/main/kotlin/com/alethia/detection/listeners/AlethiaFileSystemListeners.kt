package com.alethia.detection.listeners

import com.alethia.detection.AlethiaEventHandler
import com.alethia.model.DetectionEvent
import com.alethia.model.EventType
import com.alethia.utils.getRepoRoot
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
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
        val projectBasePath = project.basePath ?: return

        events
            .filterIsInstance<VFilePropertyChangeEvent>()
            .filter { it.propertyName == VirtualFile.PROP_NAME }
            .filter { !it.file.isDirectory }
            .filter { it.file.path.startsWith(projectBasePath) }
            .forEach { event ->
                val repoRoot = getRepoRoot(project, event.file.path) ?: return@forEach
                handler.submit(
                    DetectionEvent(
                        eventType = EventType.FILE_RENAME,
                        repoRoot = repoRoot,
                        oldPath = event.oldValue as String,
                        newPath = event.newValue as String
                    )
                )
            }
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
        val projectBasePath = project.basePath ?: return

        events
            .filterIsInstance<VFilePropertyChangeEvent>()
            .filter { it.propertyName == VirtualFile.PROP_NAME }
            .filter { it.file.isDirectory }
            .filter { it.file.path.startsWith(projectBasePath) }
            .forEach { event ->
                val repoRoot = getRepoRoot(project, event.file.path) ?: return@forEach
                handler.submit(
                    DetectionEvent(
                        eventType = EventType.DIRECTORY_RENAME,
                        repoRoot = repoRoot,
                        oldPath = event.oldValue as String,
                        newPath = event.newValue as String
                    )
                )
            }
    }
}