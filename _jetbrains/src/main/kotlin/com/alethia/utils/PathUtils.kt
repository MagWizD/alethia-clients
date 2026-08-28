package com.alethia.utils

import com.intellij.openapi.vfs.LocalFileSystem
import git4idea.repo.GitRepositoryManager
import com.intellij.openapi.project.Project

/**
 * Strips the machine-specific prefix from a file path, returning
 * only the path relative to the repository root.
 * Prevents leaking developer usernames and machine paths into
 * git notes.
 *
 * Example:
 * /Users/Johndoe/projects/my-repo/src/auth.kt -> src/auth.kt
 *
 * @param absolutePath  The full absolute path from the IDE
 * @param repoRoot      The absolute path to the repo root
 * @return              Repo-relative file path
 */
fun scrubPath(absolutePath: String, repoRoot: String): String {
    return absolutePath
        .removePrefix(repoRoot)
        .trimStart('/', '\\')
}

/**
 * Gets the root path of the git repository containing the
 * given file path. If not in a repo, it returns null.
 *
 * @param project   The current IntelliJ project
 * @param filePath  The absolute path to the file to look up
 * @return          The absolute path to the repo root, or null
 */
fun getRepoRoot(project: Project, filePath: String): String? {
    val virtualFile = LocalFileSystem.getInstance()
        .findFileByPath(filePath) ?: return null
    return GitRepositoryManager.getInstance(project)
        .getRepositoryForFile(virtualFile)?.root?.path
}