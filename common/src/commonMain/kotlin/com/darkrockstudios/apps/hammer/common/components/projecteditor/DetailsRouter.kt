package com.darkrockstudios.apps.hammer.common.components.projecteditor

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.darkrockstudios.apps.hammer.common.components.projecteditor.drafts.DraftCompare
import com.darkrockstudios.apps.hammer.common.components.projecteditor.drafts.DraftCompareComponent
import com.darkrockstudios.apps.hammer.common.components.projecteditor.drafts.DraftsList
import com.darkrockstudios.apps.hammer.common.components.projecteditor.drafts.DraftsListComponent
import com.darkrockstudios.apps.hammer.common.components.projecteditor.sceneeditor.SceneEditor
import com.darkrockstudios.apps.hammer.common.components.projecteditor.sceneeditor.SceneEditorComponent
import com.darkrockstudios.apps.hammer.common.data.MenuDescriptor
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.data.drafts.DraftDef
import kotlinx.coroutines.flow.MutableSharedFlow

internal class DetailsRouter(
	componentContext: ComponentContext,
	private val selectedSceneItem: MutableSharedFlow<SceneItem?>,
	private val removeMenu: (id: String) -> Unit,
	private val closeDetails: () -> Unit,
	private val addMenu: (menu: MenuDescriptor) -> Unit,
) {

	private val navigation = StackNavigation<Config>()

	private val stack = componentContext.childStack(
		source = navigation,
		initialConfiguration = Config.None,
		key = "DetailsRouter",
		childFactory = ::createChild
	)

	val state: Value<ChildStack<Config, ProjectEditor.ChildDestination.Detail>>
		get() = stack

	private fun createChild(
		config: Config,
		componentContext: ComponentContext
	): ProjectEditor.ChildDestination.Detail =
		when (config) {
			is Config.None -> ProjectEditor.ChildDestination.Detail.None
			is Config.SceneEditor -> ProjectEditor.ChildDestination.Detail.EditorDestination(
				sceneEditor(componentContext = componentContext, sceneDef = config.sceneDef)
			)

			is Config.DraftsList -> ProjectEditor.ChildDestination.Detail.DraftsDestination(
				draftsList(componentContext = componentContext, sceneDef = config.sceneDef)
			)

			is Config.DraftCompare -> ProjectEditor.ChildDestination.Detail.DraftCompareDestination(
				draftCompare(
					componentContext = componentContext,
					sceneDef = config.sceneDef,
					draftDef = config.draftDef
				)
			)
		}

	private fun sceneEditor(componentContext: ComponentContext, sceneDef: SceneItem): SceneEditor =
		SceneEditorComponent(
			componentContext = componentContext,
			originalSceneItem = sceneDef,
			addMenu = addMenu,
			removeMenu = removeMenu,
			closeSceneEditor = closeDetails,
			showDraftsList = ::showDraftsList
		)

	private fun draftsList(componentContext: ComponentContext, sceneDef: SceneItem): DraftsList =
		DraftsListComponent(
			componentContext = componentContext,
			sceneItem = sceneDef,
			closeDrafts = ::closeDrafts,
			compareDraft = ::compareDraft
		)

	private fun draftCompare(
		componentContext: ComponentContext,
		sceneDef: SceneItem,
		draftDef: DraftDef
	): DraftCompare =
		DraftCompareComponent(
			componentContext = componentContext,
			sceneItem = sceneDef,
			draftDef = draftDef,
			cancelCompare = ::cancelDraftCompare,
			backToEditor = ::backToEditor
		)

	fun showScene(sceneDef: SceneItem) {
		navigation.navigate(
			transformer = { stack ->
				stack.dropLastWhile { it !is Config.None }
					.plus(
						Config.SceneEditor(
							sceneDef = sceneDef
						)
					)
			},
			onComplete = { _, _ -> }
		)
	}

	private fun showDraftsList(sceneDef: SceneItem) {
		navigation.push(
			Config.DraftsList(
				sceneDef = sceneDef
			)
		)
	}

	private fun compareDraft(sceneDef: SceneItem, draftDef: DraftDef) {
		navigation.push(
			Config.DraftCompare(
				sceneDef = sceneDef,
				draftDef = draftDef
			)
		)
	}

	private fun closeDrafts() {
		navigation.popWhile { it is Config.DraftsList }
	}

	private fun cancelDraftCompare() {
		navigation.popWhile { it is Config.DraftCompare }
	}

	private fun backToEditor() {
		navigation.popWhile { it !is Config.SceneEditor }
	}

	fun onBack() {
		navigation.pop()
	}

	fun isAtRoot(): Boolean {
		return stack.active.configuration is Config.SceneEditor ||
				stack.active.configuration is Config.None
	}

	fun closeScene() {
		navigation.popWhile { it !is Config.None }
		selectedSceneItem.tryEmit(null)
	}

	fun isShown(): Boolean =
		when (stack.value.active.configuration) {
			is Config.None -> false
			else -> true
		}

	sealed class Config : Parcelable {
		@Parcelize
		object None :
			Config()

		@Parcelize
		data class SceneEditor(val sceneDef: SceneItem) :
			Config()

		@Parcelize
		data class DraftsList(val sceneDef: SceneItem) :
			Config()

		@Parcelize
		data class DraftCompare(val sceneDef: SceneItem, val draftDef: DraftDef) :
			Config()
	}
}