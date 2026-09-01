package com.alethia.config

/**
 * Central location for all Alethia constants.
 * Strings that appear in multiple places or represent
 * configurable valuse are defined here to avoid duplication.
 */
object AlethiaConstants {

    // GIT NOTES ===========================================
    /** Git notes ref namespace used by Alethia */
    const val NOTES_REF = "refs/notes/alethia"

    /** Temp ref used when fetching remote notes for merging */
    const val NOTES_REMOTE_REF = "refs/notes/alethia-remote"

    // PRE-PUSH HOOK ===========================================
    /** Environment variable used to prevent recursive hooks calls */
    const val PUSH_GUARD_VAR = "ALETHIA_PUSHING_NOTES"

    /** Marker line written into config files to detect existing installations */
    const val HOOK_MARKER = "# alethia-managed"

    /** Path to git hooks directory relative to repo root */
    const val HOOKS_DIR = ".git/hooks"

    /** Name of the pre-push hook file */
    const val PRE_PUSH_HOOK = "pre-push"

    // GIT CONFIG ===========================================
    /** Git config key, tell git which notes ref to copy on rewrite */
    const val CONFIG_REWRITE_REF = "notes.rewriteRef"

    /** Git config key, copy notes when rebasing */
    const val CONFIG_REWRITE_REBASE = "notes.rewrite.rebase"

    /** Git config key, copy notes when amending */
    const val CONFIG_REWRITE_AMEND = "notes.rewrite.amend"

    // STATE AND SNAPSHOTS ===========================================
    /** Path to the Alethia state file relative to repo root */
    const val STATE_FILE_PATH = ".idea/alethia-state.xml"

    /** Path to snapshots directory relative to repo root */
    const val SNAPSHOTS_DIR = ".alethia/snapshots"

    /** Prefix for snapshot file names */
    const val SNAPSHOTS_PREFIX = "alethia-state-"

    // PROPERTIESCOMPONENT KEYS ===========================================
    /** Key used to store the lastCommitSha in PropertiesComponent */
    const val LAST_COMMIT_SHA_KEY = "com.alethia.lastCommitSha"

    // PLUGINS ===========================================
    /** Current plugin version */
    val PLUGIN_VERSION: String
        get() = com.intellij.ide.plugins.PluginManagerCore
            .getPlugin(com.intellij.openapi.extensions.PluginId.getId("com.alethia.plugin"))
            ?.version ?: "unknown"

}