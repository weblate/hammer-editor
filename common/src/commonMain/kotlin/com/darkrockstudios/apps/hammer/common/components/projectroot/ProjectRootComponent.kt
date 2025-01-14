package com.darkrockstudios.apps.hammer.common.components.projectroot

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.darkrockstudios.apps.hammer.common.components.ProjectComponentBase
import com.darkrockstudios.apps.hammer.common.data.*
import com.darkrockstudios.apps.hammer.common.data.notesrepository.NotesRepository
import com.darkrockstudios.apps.hammer.common.data.projecteditorrepository.ProjectEditorRepository
import com.darkrockstudios.apps.hammer.common.data.projectsync.ClientProjectSynchronizer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.core.component.getScopeId

class ProjectRootComponent(
	componentContext: ComponentContext,
	projectDef: ProjectDef,
	private val addMenu: (menu: MenuDescriptor) -> Unit,
	private val removeMenu: (id: String) -> Unit,
) : ProjectComponentBase(projectDef, componentContext), ProjectRoot {

	private val synchronizer: ClientProjectSynchronizer by projectInject()
	private val projectEditor: ProjectEditorRepository by projectInject()
	private val notes: NotesRepository by projectInject()

	init {
		projectEditor.subscribeToBufferUpdates(null, scope) {
			updateCloseConfirmRequirement()
		}

		scope.launch {
			initializeProjectScope(projectDef)
		}
	}

	private val _backEnabled = MutableValue(true)
	override val backEnabled = _backEnabled

	private val _shouldConfirmClose = MutableValue(false)
	override val shouldConfirmClose = _shouldConfirmClose

	private val router = ProjectRootRouter(
		componentContext,
		projectDef,
		addMenu,
		removeMenu,
		::updateCloseConfirmRequirement,
		::showProjectSync,
		scope,
		dispatcherMain
	)

	private val modalRouter = ProjectRootModalRouter(
		componentContext,
		projectDef
	)

	override val routerState: Value<ChildStack<*, ProjectRoot.Destination<*>>>
		get() = router.state

	override val modalRouterState: Value<ChildSlot<ProjectRootModalRouter.Config, ProjectRoot.ModalDestination>>
		get() = modalRouter.state

	override fun showEditor() {
		router.showEditor()
	}

	override fun showNotes() {
		Napier.d("showNotes component")
		router.showNotes()
	}

	override fun showEncyclopedia() {
		router.showEncyclopedia()
	}

	override fun showHome() {
		router.showHome()
	}

	override fun showTimeLine() {
		router.showTimeLine()
	}

	override fun showDestination(type: ProjectRoot.DestinationTypes) {
		when (type) {
			ProjectRoot.DestinationTypes.Editor -> showEditor()
			ProjectRoot.DestinationTypes.Notes -> showNotes()
			ProjectRoot.DestinationTypes.Encyclopedia -> showEncyclopedia()
			ProjectRoot.DestinationTypes.TimeLine -> showTimeLine()
			ProjectRoot.DestinationTypes.Home -> showHome()
		}
	}

	override fun hasUnsavedBuffers(): Boolean {
		return projectEditor.hasDirtyBuffers()
	}

	override suspend fun storeDirtyBuffers() {
		projectEditor.storeAllBuffers()
	}

	override fun isAtRoot() = router.isAtRoot() && modalRouter.isAtRoot()

	override fun showProjectSync() = modalRouter.showProjectSync()

	override fun dismissProjectSync() = modalRouter.dismissProjectSync()

	private fun updateCloseConfirmRequirement() {
		_shouldConfirmClose.value = hasUnsavedBuffers() && router.isAtRoot()
		_backEnabled.value = router.isAtRoot()
	}

	override fun onDestroy() {
		super.onDestroy()
		Napier.i { "ProjectRootComponent closing Project Editor" }

		closeProjectScope(getKoin().getScope(projectScope.getScopeId()), projectDef)
	}

	override fun onStart() {
		super.onStart()
		addMenuItems()
	}

	override fun onStop() {
		super.onStop()
		removeMenuItems()
	}

	private fun addMenuItems() {
		if (synchronizer.isServerSynchronized()) {
			addMenu(
				MenuDescriptor(
					id = "project-root-sync",
					label = "Sync",
					items = listOf(
						MenuItemDescriptor(
							id = "project-root-sync-start",
							label = "Start",
							icon = "",
							shortcut = KeyShortcut(keyCode = 0x72),
							action = { showProjectSync() }
						)
					)
				)
			)
		}
	}

	private fun removeMenuItems() {
		if (synchronizer.isServerSynchronized()) {
			removeMenu("project-root-sync")
		}
	}
}