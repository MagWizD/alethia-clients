package com.alethia.startup

import com.alethia.startup.AlethiaInstaller
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.logging.Logger

class AlethiaStartupActivity: ProjectActivity {

    private val LOG = Logger.getLogger(AlethiaStartupActivity::class.java.name)

    /**
     * Called by IntelliJ after the project has fully loaded.
     * Using postStartupActivity ensures git repos are available
     * before we even attempt to access them.
     *
     * @param project The project that just opened
     */
    override suspend fun execute(project: Project) {
        LOG.info("AlethiaStartupActivity: project opened - running installer")
        AlethiaInstaller.install(project)
    }
}
