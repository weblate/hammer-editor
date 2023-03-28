package com.darkrockstudios.apps.hammer.common.data.projectsync

import com.darkrockstudios.apps.hammer.base.http.ApiProjectEntity
import com.darkrockstudios.apps.hammer.base.http.ApiSceneType
import com.darkrockstudios.apps.hammer.base.http.synchronizer.EntityConflictException
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.SceneContent
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.data.drafts.SceneDraftRepository
import com.darkrockstudios.apps.hammer.common.data.projecteditorrepository.ProjectEditorRepository
import com.darkrockstudios.apps.hammer.common.server.ServerProjectApi
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel

class SceneSynchronizer(
	private val projectDef: ProjectDef,
	private val projectEditorRepository: ProjectEditorRepository,
	private val draftRepository: SceneDraftRepository,
	private val serverProjectApi: ServerProjectApi
) : EntitySynchronizer<ApiProjectEntity.SceneEntity> {

	val conflictResolution = Channel<ApiProjectEntity.SceneEntity>()

	override suspend fun uploadEntity(
		id: Int,
		syncId: String,
		onConflict: EntityConflictHandler<ApiProjectEntity.SceneEntity>
	) {
		Napier.d("Uploading Scene $id")

		val scene =
			projectEditorRepository.getSceneItemFromId(id) ?: throw IllegalStateException("Scene missing for ID $id")
		val contents = projectEditorRepository.loadSceneMarkdownRaw(scene)
		val path = projectEditorRepository.getPathSegments(scene)

		val result = serverProjectApi.uploadScene(scene, path, contents, syncId)
		if (!result.isSuccess) {
			result.exceptionOrNull()?.let { e -> Napier.e("Failed to upload scene", e) }
		} else {
			val exception = result.exceptionOrNull()
			val conflictException = exception as? EntityConflictException.SceneConflictException
			if (conflictException != null) {
				Napier.w("Conflict for scene $id detected")
				onConflict(conflictException.entity)

				val resolvedEntity = conflictResolution.receive()
				val resolvedScene = SceneItem(
					projectDef = projectDef,
					id = resolvedEntity.id,
					name = resolvedEntity.name,
					order = resolvedEntity.order,
					type = resolvedEntity.sceneType.toSceneType(),
				)

				val resolvedPath = projectEditorRepository.getPathSegments(resolvedScene)
				val resolvedContent = resolvedEntity.content
				val resolveResult = serverProjectApi.uploadScene(scene, resolvedPath, resolvedContent, syncId, true)

				if (!resolveResult.isSuccess) {
					resolveResult.exceptionOrNull()?.let { e -> Napier.e("Failed to upload scene", e) }
				} else {
					Napier.d("Resolved conflict for scene $id")
				}
			} else {
				Napier.e("Failed to upload scene", exception)
			}
		}
	}

	override suspend fun downloadEntity(id: Int, syncId: String) {
		Napier.d("Downloading Scene $id")

		val tree = projectEditorRepository.rawTree

		val result = serverProjectApi.downloadScene(projectDef, id, syncId)
		if (result.isSuccess) {
			val apiScene = result.getOrNull() ?: throw IllegalStateException("Scene missing for ID $id")

			val parentId = apiScene.path.lastOrNull()
			val parent = if (parentId != null) {
				projectEditorRepository.getSceneItemFromId(parentId)
			} else {
				null
			}

			if (apiScene.sceneType == ApiSceneType.Scene) {
				Napier.d("Downloading Scene $id")

				val existingScene = projectEditorRepository.getSceneItemFromId(id)
				val sceneItem = if (existingScene != null) {
					val existingTreeNode = tree.find { it.id == id }
					// Must move parents
					if (existingTreeNode.parent?.value?.order != apiScene.path.lastOrNull()) {
						existingTreeNode.parent?.removeChild(existingTreeNode)

						val newParent = tree.find { it.id == apiScene.path.lastOrNull() }
						newParent.addChild(existingTreeNode)
					}

					existingScene
				} else {
					projectEditorRepository.createScene(parent = parent, sceneName = apiScene.name)
						?: throw IllegalStateException("Failed to create scene")
				}

				val treeNode = tree.find { it.id == id }
				treeNode.value = sceneItem.copy(
					name = apiScene.name,
					order = apiScene.order
				)

				val content = SceneContent(sceneItem, apiScene.content)
				projectEditorRepository.onContentChanged(content)
			} else {
				Napier.d("Downloading Group $id")

				val existingGroup = projectEditorRepository.getSceneItemFromId(id)
				val sceneItem = if (existingGroup != null) {
					val existingTreeNode = tree.find { it.id == id }
					// Must move parents
					if (existingTreeNode.parent?.value?.order != apiScene.path.lastOrNull()) {
						existingTreeNode.parent?.removeChild(existingTreeNode)

						val newParent = tree.find { it.id == apiScene.path.lastOrNull() }
						newParent.addChild(existingTreeNode)
					}

					existingGroup
				} else {
					projectEditorRepository.createGroup(parent = parent, groupName = apiScene.name)
						?: throw IllegalStateException("Failed to create scene")
				}

				val treeNode = tree.find { it.id == id }
				treeNode.value = sceneItem.copy(
					name = apiScene.name,
					order = apiScene.order
				)
			}
		} else {
			result.exceptionOrNull()?.let { e -> Napier.e("Failed to download scene", e) }
		}
	}

	override suspend fun reIdEntity(oldId: Int, newId: Int) {
		Napier.d("Re-Id Scene $oldId to $newId")

		projectEditorRepository.reIdScene(oldId, newId)

		draftRepository.reIdScene(
			oldId = oldId,
			newId = newId,
			projectDef = projectDef
		)
	}

	override suspend fun finalizeSync() {
		projectEditorRepository.rationalizeTree()
		projectEditorRepository.cleanupSceneOrder()
		projectEditorRepository.storeAllBuffers()
	}
}
