package com.alethia.services

import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.SimpleFormatter

class LoggingFactory : LoggingService {

    init {
        // Get log directory and file path
        val logDir = File(System.getProperty("user.home"), ".alethia")
        // Create the directory if it doesn't exist yet
        logDir.mkdirs()

        val logFile = File(logDir, "alethia.log")

        try {
            // FileHandler writes log output to a file
            val fileHandler = FileHandler(logFile.absolutePath, true)
            // Writes human-readable lines rather than XML
            fileHandler.formatter = SimpleFormatter()
            // Accept all log levels: DEBUG through SEVERE
            fileHandler.level = Level.ALL

            // Get or create the root logger for the com.alethia package
            val alethiaLogger = LogManager.getLogManager().getLogger("com.alethia")
                ?: java.util.logging.Logger.getLogger("com.alethia")

            // Attach the file handler so all com.alethia log output
            // is written to alethia.log
            alethiaLogger.addHandler(fileHandler)
            // Set level to ALL so no messages are filtered out
            alethiaLogger.level = Level.ALL

        } catch (e: Exception) {
            // If file logging setup fails, permissions issue, disk full, etc.
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