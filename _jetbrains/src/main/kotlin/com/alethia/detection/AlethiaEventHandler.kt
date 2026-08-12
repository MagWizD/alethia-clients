package com.alethia.detection

import com.alethia.detection.events.DetectionEvent
import com.alethia.detection.events.EventSource
import com.alethia.detection.rules.RuleEngine
import com.alethia.model.FlaggedRegion
import com.alethia.services.LoggingFactory
import com.alethia.services.LoggingService
import com.alethia.session.AlethiaStateService
import com.alethia.session.SessionState
import com.alethia.utils.scrubPath
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Single entry point for all detection events.
 * Registered as project-scoped service so listeners can get
 * it thorugh project.service<AlethiaEventHandler>() without wiring
 * dependencies themselves (otherwise every listener added would need
 * to instantiate log service, sessionState, and the hnadler, real
 * annoying and messy -> tight coupling).
 *
 * Two constructors, here's why:
 * - Primary: accepts SessionState and LoggingService directly.
 *   Used in tests to inject mocks without needing the IntelliJ Platform.
 * - Secondary: accepts Project and resolves dependencies via IntelliJ
 *   service locator. Used by IntelliJ when managing this as a service.
 * Deduplication:
 * - CLIPBOARD_PASTE events are recorded per file. Any subsequent
 *   DOCUMENT_CHANGE for the same file within PASTE_WINDOW_MS is
 *   suppressed, the more specific source will win.
 */
class AlethiaEventHandler(
    private val sessionState: SessionState,
    private val logging: LoggingService
) {

    /**
     * Secondary constructor: This is used by IntelliJ when instantiating
     * this class as service. Resolves SessionState
     * and LoggingService from the IntelliJ service maps.
     *
     * @param project The currently open project, gets injected by IntelliJ
     */
    constructor(project: Project) : this(
        sessionState = project.service<AlethiaStateService>(),
        logging = service<LoggingFactory>()
    )

    // Fecth the logger from LogService
    private val LOG = logging.getLogger(AlethiaEventHandler::class.java.name)

    // Track recent paste events by file path
    // Used to suppress duplicate events for the same insertion
    private val recentPastes = mutableMapOf<String, Long>()
    private val PASTE_WINDOW_MS = 500L

    /**
     * Submit a detection event for rule evaluating.
     * Deduplicates by source priority, evaluates against
     * all registered rules, and queues a flag if one needs to be created.
     *
     * @param event The detection event from any of our listeners
     */
    fun submit(event: DetectionEvent) {

        // Record paste events so any following DOCUMENT_CHANGE
        // events for matching file get suppressed
        if (event.source == EventSource.CLIPBOARD_PASTE) {
            recentPastes[event.filePath] = System.currentTimeMillis()
        }
        // Suppress DOCUMENT_CHANGE if a paste just fired for this file
        if (event.source == EventSource.DOCUMENT_CHANGE) {
            val lastPaste = recentPastes[event.filePath] ?: 0L
            if (System.currentTimeMillis() - lastPaste < PASTE_WINDOW_MS) return
        }

        // Evaluate against all registered rules in the engine
        val result = RuleEngine.evaluate(event) ?: return

        // Build flag and queue via injected session state
        // Path is scrubbed to repo-relative be being persisted
        sessionState.addFlag(
            FlaggedRegion(
                eventType = result.eventType,
                file = scrubPath(event.filePath, event.repoRoot),
                startLine = event.startLine,
                endLine = event.endLine,
                charCount = event.charCount,
                rationale = result.rationale,
                timeStamp = event.timestamp
            )
        )

        // Log flag creation
        LOG.info("AlethiaEventHandler: flag created: ${event.filePath} L${event.startLine}-${event.endLine}")
    }
}