package com.darkrockstudios.apps.hammer.common.components.projecteditor.sceneeditor

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.getAndUpdate
import com.darkrockstudios.apps.hammer.common.components.ProjectComponentBase
import com.darkrockstudios.apps.hammer.common.data.*
import com.darkrockstudios.apps.hammer.common.data.drafts.SceneDraftRepository
import com.darkrockstudios.apps.hammer.common.data.projecteditorrepository.ProjectEditorRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock


class SceneEditorComponent(
	componentContext: ComponentContext,
	originalSceneItem: SceneItem,
	private val addMenu: (menu: MenuDescriptor) -> Unit,
	private val removeMenu: (id: String) -> Unit,
	private val closeSceneEditor: () -> Unit,
	private val showDraftsList: (SceneItem) -> Unit,
) : ProjectComponentBase(originalSceneItem.projectDef, componentContext), SceneEditor {

	private val projectEditor: ProjectEditorRepository by projectInject()
	private val draftsRepository: SceneDraftRepository by projectInject()

	private val _state = MutableValue(SceneEditor.State(sceneItem = originalSceneItem))
	override val state: Value<SceneEditor.State> = _state

	override var lastForceUpdate = MutableValue<Long>(0)

	private var bufferUpdateSubscription: Job? = null

	private val sceneDef: SceneItem
		get() = state.value.sceneItem

	init {
		loadSceneContent()

		subscribeToBufferUpdates()
	}

	private fun subscribeToBufferUpdates() {
		Napier.d { "SceneEditorComponent start collecting buffer updates" }

		bufferUpdateSubscription?.cancel()

		bufferUpdateSubscription =
			projectEditor.subscribeToBufferUpdates(sceneDef, scope, ::onBufferUpdate)
	}

	private suspend fun onBufferUpdate(sceneBuffer: SceneBuffer) = withContext(dispatcherMain) {
		_state.getAndUpdate {
			it.copy(sceneBuffer = sceneBuffer)
		}

		if (sceneBuffer.source != UpdateSource.Editor) {
			forceUpdate()
		}
	}

	override fun loadSceneContent() {
		_state.getAndUpdate {
			val buffer = projectEditor.loadSceneBuffer(sceneDef)
			it.copy(sceneBuffer = buffer)
		}
	}

	override suspend fun storeSceneContent() =
		projectEditor.storeSceneBuffer(sceneDef)

	override fun onContentChanged(content: PlatformRichText) {
		projectEditor.onContentChanged(
			SceneContent(
				scene = sceneDef,
				platformRepresentation = content
			),
			UpdateSource.Editor
		)
	}

	override fun addEditorMenu() {
		val closeItem = MenuItemDescriptor("scene-editor-close", "Close", "") {
			Napier.d("Scene close selected")
			closeSceneEditor()
		}

		val saveItem = MenuItemDescriptor(
			"scene-editor-save",
			"Save",
			"",
			KeyShortcut(83, ctrl = true)
		) {
			Napier.d("Scene save selected")
			scope.launch { storeSceneContent() }
		}

		val discardItem = MenuItemDescriptor(
			"scene-editor-discard",
			"Discard",
			""
		) {
			Napier.d("Scene buffer discard selected")
			projectEditor.discardSceneBuffer(sceneDef)
			forceUpdate()
		}

		val renameItem = MenuItemDescriptor(
			"scene-editor-rename",
			"Rename",
			""
		) {
			Napier.d("Scene rename selected")
			beginSceneNameEdit()
		}

		val draftsItem = MenuItemDescriptor(
			"scene-editor-view-drafts",
			"Drafts",
			""
		) {
			Napier.i("View drafts")
			showDraftsList(sceneDef)
		}

		val saveDraftItem = MenuItemDescriptor(
			"scene-editor-save-draft",
			"Save Draft",
			""
		) {
			Napier.i("Save draft")
			beginSaveDraft()
		}

		val menu = MenuDescriptor(
			getMenuId(),
			"Scene",
			listOf(renameItem, saveItem, discardItem, draftsItem, saveDraftItem, closeItem)
		)
		addMenu(menu)
	}

	private fun forceUpdate() {
		lastForceUpdate.value = Clock.System.now().epochSeconds
	}

	override fun removeEditorMenu() {
		removeMenu(getMenuId())
	}

	override fun beginSceneNameEdit() {
		_state.getAndUpdate { it.copy(isEditingName = true) }
	}

	override fun endSceneNameEdit() {
		_state.getAndUpdate { it.copy(isEditingName = false) }
	}

	override suspend fun changeSceneName(newName: String) {
		endSceneNameEdit()
		projectEditor.renameScene(sceneDef, newName)

		_state.getAndUpdate {
			it.copy(
				sceneItem = it.sceneItem.copy(name = newName)
			)
		}
	}

	override fun beginSaveDraft() {
		_state.getAndUpdate { it.copy(isSavingDraft = true) }
	}

	override fun endSaveDraft() {
		_state.getAndUpdate { it.copy(isSavingDraft = false) }
	}

	override suspend fun saveDraft(draftName: String): Boolean {
		return if (SceneDraftRepository.validDraftName(draftName)) {
			val draftDef = draftsRepository.saveDraft(
				sceneDef,
				draftName
			)
			if (draftDef != null) {
				Napier.i { "Draft Saved: ${draftDef.draftTimestamp}" }
				true
			} else {
				Napier.e { "Failed to save Draft!" }
				false
			}
		} else {
			Napier.w { "Failed to save Draft, invalid name: $draftName" }
			false
		}
	}

	private fun getMenuId(): String {
		return "scene-editor-${sceneDef.id}-${sceneDef.name}"
	}

	override fun onStart() {
		addEditorMenu()
	}

	override fun onStop() {
		removeEditorMenu()
	}
}