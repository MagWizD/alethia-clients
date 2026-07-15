package com.alethia.services

import com.intellij.openapi.diagnostic.Logger

/**
 * Production implementation of LoggingService.
 * Wraps IntelliJ's Logger.getInstance() the same way
 * LoggingFactory wraps Log4j's LogManager in Spring Boot.
 * Registered as a service in plugin.xml so it can be
 * injected via constructor injection.
 */
class LoggingFactory : LoggingService {
    override fun getLogger(name: String): Logger {
        return Logger.getInstance(name)
    }
}