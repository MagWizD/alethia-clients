package com.alethia.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsMappingListener
import git4idea.repo.GitRepositoryManager
import org.slf4j.LoggerFactory


/**
 * Runs automatically when a project opens in the IDE.
 * Registered in plugin.xml as a postStartupActivity.
 *
 * Waits for VCS repository mapping to complete before running
 * the installer, git repos are not immediately available when
 * a project first opens. GitRepositoryManager.repositories returns
 * empty until git4idea finishes scanning the project directories.
 * Subscribes to VCS_CONFIGURATION_CHANGED and runs AlethiaInstaller
 * only once repositories are confirmed available.
 */
class AlethiaStartupActivity: ProjectActivity {

    private val LOG = LoggerFactory.getLogger(AlethiaStartupActivity::class.java)

    /**
     * Called by IntelliJ after the project has fully loaded.
     * Using postStartupActivity ensures that the code runs on
     * project load.
     *
     * GitRepositoryManager and VcsMappingListener ensure that
     * if any repos are in the project they are fully mapped
     * before calling AlethiaInstaller. Otherwise, installer
     * will not set up the necessary Alethia configurations.
     *
     * @param project The project that just opened
     */
    override suspend fun execute(project: Project) {
        // Load logging configuration
        // Searches the module (com.alethia) for a resourse with name "logging.properties",
        // the provides the found resource, if not null (hence the '?'), to the LogManager
        // which sets the log levels, format, log output directory, etc.
        val configStream = AlethiaStartupActivity::class.java
            .getResourceAsStream("/logging.properties")
        configStream?.let {
            java.util.logging.LogManager.getLogManager().readConfiguration(it)
            it.close()
        }

        LOG.info("AlethiaStartupActivity: project opened - attempt to install")

        // Retrieve the Repo Manager to de
        val manager = GitRepositoryManager.getInstance(project)

        // If repos are already available run immediately
        if (manager.repositories.isNotEmpty()) {
            LOG.info("AlethiaStartupActivity: installer starting")
            AlethiaInstaller.install(project)
            return
        }

        // Otherwise wiat for git to finish mapping repos
        LOG.info("AlethiaStartupActivity: no repos found - wait for repo map to complete")
        project.messageBus.connect().subscribe(
            ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED,
            VcsMappingListener {
                // Give git a moment to finish initializing repos after mapping changes
                Thread.sleep(1000)
                if (manager.repositories.isNotEmpty()) {
                    LOG.info("AlethiaStartupActivity: installer starting")
                    AlethiaInstaller.install(project)
                } else {
                    LOG.warn("AlethiaStartupActivity: repos still empty after mapping - skipping")
                }
            }
        )
    }
}
