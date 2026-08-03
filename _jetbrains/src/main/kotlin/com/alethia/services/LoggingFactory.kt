package com.alethia.services

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.SimpleFormatter

/**
 * Concrete implementation of LoggingService.
 * Sets up file logging for all Alethia classes when first created by IntelliJ.
 * All log output from com.alethia.* is written to ~/.alethia/alethia.log
 *
 * NOTE: If logs appear to not be working check the JetBrains log file -> idea.log
 * This file can typically be found in the .intellijplatform/sandbox folder or inside
 * the sandbox environment you can go to Help -> Show log in explorer. This opens the
 * file explorer, open up the idea.log and check for:
 *
 *      LoggingFactory: could not set up file logging -> <error_message_here>
 */
class LoggingFactory : LoggingService {

    /**
     * Runs once when IntelliJ creates this service.
     * Creates the log directory and wires all com.alethia loggers
     * to write to alethia.log in addition to the default output.
     */
    init {
        // Get log directory and file path
        val logDir = File(System.getProperty("user.home"), ".alethia")
        // Create the alethia directory and log file if it doesn't exist yet
        logDir.mkdirs()
        val logFile = File(logDir, "alethia.log")

        try {
            // Set up the fileHandler so that we can format our logs as they come in to the file
            val fileHandler = FileHandler(logFile.absolutePath, true)
            // Writes human-readable lines rather than XML
            fileHandler.formatter = SimpleFormatter()
            fileHandler.level = Level.ALL

            // Get or create the root logger for the com.alethia package
            val alethiaLogger = LogManager.getLogManager().getLogger("com.alethia")
                ?: java.util.logging.Logger.getLogger("com.alethia")

            // Attach the file handler so all com.alethia log output
            // is written to alethia.log
            alethiaLogger.addHandler(fileHandler)
            alethiaLogger.level = Level.ALL
        } catch (e: Exception) {
            // If file logging setup fails (Use IntelliJ logger to let us know, not great solution, but it works)
            Logger.getInstance(LoggingFactory::class.java)
                .warn("LoggingFactory: could not set up file logging -> ${e.message}")
        }
    }

    /**
     * Returns an IntelliJ Platform Logger for the given name.
     * For example: AlethiaEventHandler::class.java.name
     *
     * @param name  The logger name
     * @return      IntelliJ Platform Logger instance
     */
    override fun getLogger(name: String): java.util.logging.Logger {
        return java.util.logging.Logger.getLogger(name)
    }
}