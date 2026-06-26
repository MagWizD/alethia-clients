package com.alethia.session

import com.alethia.model.SerializableFlaggedRegion
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * Persistent storage service for Alethia session data.
 * Handles two types of persistence:
 *
 * -    FlaggedRegion - Uses PropertiesStateComponent, serialized to
 *      alethia-state.xml in IDE config directory. Survives restarts
 *      but not crashes, writes on IDE close. However, it enables us
 *      to store structured data unlike PropertiesComponent
 *
 * -    lastCommitSha - Uses PropertiesComponent, writes immediately
 *      on every update. Crash safe, which is great for saving last
 *      commit SHA and prevent duplicate note generation. However,
 *      it is unable to accept complex data, must be a simple data
 *      type such as string or int
 *
 * The service will be registered in the plugins.xml and managed by the
 * IntelliJ Platform. We can access it via service<AlethiaStateService>,
 * we never need to instantiate it, it is a singleton service.
 */
@Service(Service.Level.PROJECT)
@State(
    name ="AlethiaStateService",                        // Name inside the XML
    storages = [Storage("alethia-state.xml")]   // Filename
)
class AlethiaStateService(private val project : Project) :
    PersistentStateComponent<AlethiaStateService.State> {

    // -------------- Set Service constants/variables ------------------

    /**
     * Serialization state container for PersistentStateComponent
     * IntelliJ requires is to use mutable fields with defaults to
     * serialize to XML automatically. We use a SerializableFlaggedRegion
     * rather than the FlaggedRegion because data classes with constructors
     * do not auto-serialize.
      */
    class State {
        var flaggedRegions: MutableList<SerializableFlaggedRegion> = mutableListOf()
    }
    // Current in-memory state, IntelliJ will read from/write to this first
    private var myState = State()
    // Key for storing the last commit SHA in PropertiesComponent
    // This 'com.alethia' is a namespace to avoid naming conflicts with other plugins
    private val LAST_COMMIT_SHA_KEY = "com.alethia.lastcommitsha"
    // PropertiesComponent instance, application level key/value store
    // Writes immediately to disk on every setValue call.
    // Pass in the project so that we don't conflict with other repos
    private val props = PropertiesComponent.getInstance(project)

    // -------------- Implement PersistentStateComponent ------------------

    /**
     * Called by IntelliJ when it needs to save data to disk.
     * Returns the current state object for serialization.
     */
    override fun getState(): State = myState

    /**
     * Called by IntelliJ when state is loaded from disk on startup.
     * Replaces the current in-memory state with restored data from
     * the disk.
     */
    override fun loadState(state: State) {
        // Restore current state to state loaded from disk
        myState = state
    }

    // -------------- Implement the FlaggedRegion API ------------------

    // Implement functions to update flags within the current state

    // -------------- Implement the lastCommitSha API ------------------

    // Implement functions to load and restore last commit sha
}
