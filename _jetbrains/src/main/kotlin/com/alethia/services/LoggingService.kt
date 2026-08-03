package com.alethia.services

/**
 * Interface for logging -  abstracted so it can be mocked in tests.
 */
interface LoggingService {
    fun getLogger(name: String): java.util.logging.Logger
}