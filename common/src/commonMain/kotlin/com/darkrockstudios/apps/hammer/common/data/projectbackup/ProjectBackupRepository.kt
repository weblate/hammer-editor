package com.darkrockstudios.apps.hammer.common.data.projectbackup

import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.projectsrepository.ProjectsRepository
import com.darkrockstudios.apps.hammer.common.fileio.HPath
import com.darkrockstudios.apps.hammer.common.fileio.okio.toHPath
import com.darkrockstudios.apps.hammer.common.fileio.okio.toOkioPath
import com.darkrockstudios.apps.hammer.common.util.format
import kotlinx.datetime.*
import okio.FileSystem
import okio.Path
import org.koin.core.component.KoinComponent

abstract class ProjectBackupRepository(
	protected val fileSystem: FileSystem,
	protected val projectsRepository: ProjectsRepository,
	protected val clock: Clock
) : KoinComponent {

	fun getBackupsDirectory(): HPath {
		val dir = (projectsRepository.getProjectsDirectory().toOkioPath() / BACKUP_DIRECTORY)

		if (fileSystem.exists(dir).not()) {
			fileSystem.createDirectories(dir)
		}

		return dir.toHPath()
	}

	fun getBackups(): List<ProjectBackupDef> {
		val dir = getBackupsDirectory().toOkioPath()
		return fileSystem.list(dir)
			.filter { fileSystem.metadata(it).isRegularFile }
			.mapNotNull { path -> getProjectBackupDef(path) }
			.sortedBy { it.date }
	}

	fun getBackupsForProject(projectDef: ProjectDef): List<ProjectBackupDef> {
		return getBackups().filter { backup -> backup.projectDef == projectDef }
	}

	private fun backupNameToProjectName(backupName: String): String {
		return backupName.replace("_", " ")
	}

	protected fun projectNameToBackupName(projectName: String): String {
		return projectName.replace(" ", "_")
	}

	protected fun createNewProjectBackupDef(projectDef: ProjectDef): ProjectBackupDef {
		val path = pathForBackup(projectDef.name, clock.now())

		return ProjectBackupDef(
			path = path,
			projectDef = projectDef,
			date = clock.now()
		)
	}

	private fun pathForBackup(projectName: String, date: Instant): HPath {
		val filename = filenameForBackup(projectName, date)
		val dir = getBackupsDirectory().toOkioPath()
		return (dir / filename).toHPath()
	}

	private fun filenameForBackup(projectName: String, date: Instant): String {
		val backupName = projectNameToBackupName(projectName)
		val dateStr = date.toBackupDate()
		return "$backupName-$dateStr.zip"
	}

	protected fun getProjectBackupDef(path: Path): ProjectBackupDef? {
		val match = FILE_NAME_PATTERN.matchEntire(path.name)
		return if (match != null) {
			val backupName = match.groups[1]?.value
			val date = match.groups[2]?.value

			if (backupName != null && date != null) {
				val projectName = backupNameToProjectName(backupName)

				val dateInstant = localDateTime(date).toInstant(TimeZone.UTC)
				val projectDir = projectsRepository.getProjectDirectory(projectName)

				val projectDef = ProjectDef(
					name = projectName,
					path = projectDir
				)

				ProjectBackupDef(
					path = path.toHPath(),
					projectDef = projectDef,
					date = dateInstant
				)
			} else {
				null
			}
		} else {
			null
		}
	}

	abstract fun supportsBackup(): Boolean

	abstract suspend fun createBackup(projectDef: ProjectDef): ProjectBackupDef?

	private fun localDateTime(dateTimeStr: String): LocalDateTime {
		val match = DATE_PATTERN.matchEntire(dateTimeStr) ?: throw IllegalArgumentException("Failed to parse date time")
		val year = match.groupValues[1].toInt()
		val month = match.groupValues[2].toInt()
		val day = match.groupValues[3].toInt()
		val hour = match.groupValues[4].toInt()
		val minute = match.groupValues[5].toInt()
		val second = match.groupValues[6].toInt()

		return LocalDateTime(
			date = LocalDate(
				year = year,
				monthNumber = month,
				dayOfMonth = day
			),
			time = LocalTime(hour, minute, second)
		)
	}

	companion object {
		const val BACKUP_DIRECTORY = ".backups"
		val FILE_NAME_PATTERN = Regex("^([a-zA-Z0-9_]+)-(\\d{4}-[a-zA-Z]{3}-\\d{2}T\\d+Z)\\.zip\$")
		val DATE_PATTERN = Regex("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2})(\\d{2})(\\d{2})Z$")
	}
}

private fun Instant.toBackupDate(): String {
	val dateTime = toLocalDateTime(TimeZone.UTC)
	val dateStr = dateTime.format("YYYY-MM-dd'T'hhmmss'Z'")

	return dateStr
}