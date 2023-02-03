package com.darkrockstudios.apps.hammer.common.data.projectsrepository

import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.defaultDispatcher
import com.darkrockstudios.apps.hammer.common.fileio.HPath
import com.darkrockstudios.apps.hammer.common.projecteditor.metadata.ProjectMetadata
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope

abstract class ProjectsRepository {

    protected val projectsScope = CoroutineScope(defaultDispatcher)

    abstract fun getProjectsDirectory(): HPath
    abstract fun getProjects(projectsDir: HPath = getProjectsDirectory()): List<ProjectDef>
    abstract fun getProjectDirectory(projectName: String): HPath
    abstract fun createProject(projectName: String): Boolean
    abstract fun deleteProject(projectDef: ProjectDef): Boolean
    abstract suspend fun loadMetadata(projectDef: ProjectDef): ProjectMetadata?

    fun validateFileName(fileName: String?): Boolean {
        return if (fileName != null) {
            val wasValid = fileNameValidations.map {
                it.condition(fileName).also { isValid ->
                    if (!isValid) Napier.w { "$fileName Failed validation: ${it.name}" }
                }
            }.reduce { acc, b -> acc && b }

            Napier.i("$fileName was valid: $wasValid")

            wasValid
        } else {
            false
        }
    }

    private val fileNameValidations = listOf(
        Validator("Was Blank") { it.isNotBlank() },
        Validator("Invalid Characters") { Regex("""[\da-zA-Z _']+""").matches(it) },
    )

    private data class Validator(
        val name: String,
        val condition: (String) -> Boolean,
    )
}