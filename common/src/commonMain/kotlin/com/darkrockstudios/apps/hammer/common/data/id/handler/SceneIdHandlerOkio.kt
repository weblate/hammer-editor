package com.darkrockstudios.apps.hammer.common.data.id.handler

import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.projecteditorrepository.ProjectEditorRepository
import com.darkrockstudios.apps.hammer.common.data.projecteditorrepository.ProjectEditorRepositoryOkio
import com.darkrockstudios.apps.hammer.common.data.projecteditorrepository.filterScenePathsOkio
import com.darkrockstudios.apps.hammer.common.fileio.okio.toOkioPath
import okio.FileSystem

class SceneIdHandlerOkio(
	private val fileSystem: FileSystem
) : IdHandler {
	override fun findHighestId(projectDef: ProjectDef): Int {
		val sceneDir = ProjectEditorRepositoryOkio.getSceneDirectory(projectDef, fileSystem).toOkioPath()

		val maxId: Int = fileSystem.listRecursively(sceneDir)
			.filterScenePathsOkio().maxOfOrNull { path ->
				ProjectEditorRepository.getSceneIdFromFilename(path.name)
			} ?: -1

		return maxId
	}
}