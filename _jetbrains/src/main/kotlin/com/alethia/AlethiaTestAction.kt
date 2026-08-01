package com.alethia

import com.alethia.services.LoggingFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import java.io.File

/**
 * Temporary test action — verifies plugin classes, logging,
 * and project availability. Remove before release.
 */
class AlethiaTestAction : AnAction("Alethia Test") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val projectName = project?.name ?: "NULL"
        val projectPath = project?.basePath ?: "NULL"

        println("Project: $projectName")
        println("Project path: $projectPath")

        // Print exact path the logger would write to
        val logDir = File(System.getProperty("user.home"), ".alethia")
        val logFile = File(logDir, "alethia.log")
        println("=== LOG DIR: ${logDir.absolutePath} ===")
        println("=== LOG FILE: ${logFile.absolutePath} ===")
        println("=== LOG DIR EXISTS: ${logDir.exists()} ===")
        println("=== LOG FILE EXISTS: ${logFile.exists()} ===")

        // Try Alethia logger
        try {
            val logging = service<LoggingFactory>()
            val LOG = logging.getLogger(AlethiaTestAction::class.java.name)
            LOG.info("AlethiaTestAction fired — project=$projectName path=$projectPath")
            println("=== ALETHIA LOGGER SUCCEEDED ===")
        } catch (ex: Exception) {
            println("=== ALETHIA LOGGER FAILED: ${ex.message} ===")
        }

        Messages.showMessageDialog(
            project,
            "Project: $projectName\nPath: $projectPath",
            "Alethia Test",
            Messages.getInformationIcon()
        )
    }
}