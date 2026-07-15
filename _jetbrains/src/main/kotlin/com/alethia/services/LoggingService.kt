package com.alethia.services

import com.intellij.openapi.diagnostic.Logger

/**
 * Interface for logging -  abstracted so it can be mocked in tests.
 */
interface LoggingService {
    fun getLogger(name: String): Logger
}