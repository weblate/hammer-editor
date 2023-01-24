package com.darkrockstudios.apps.hammer.common.projectroot

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.router.stack.ChildStack
import com.darkrockstudios.apps.hammer.common.encyclopedia.EncyclopediaUi
import com.darkrockstudios.apps.hammer.common.notes.NotesUi
import com.darkrockstudios.apps.hammer.common.projecteditor.ProjectEditorUi

private val VERTICAL_CONTROL_WIDTH_THRESHOLD = 700.dp

fun getDestinationIcon(location: ProjectRoot.DestinationTypes): ImageVector {
	return when (location) {
		ProjectRoot.DestinationTypes.Editor -> Icons.Filled.Edit
		ProjectRoot.DestinationTypes.Notes -> Icons.Filled.Dock
		ProjectRoot.DestinationTypes.Encyclopedia -> Icons.Filled.Dataset
	}
}

@Composable
fun ProjectRootUi(
	component: ProjectRoot,
	padding: PaddingValues = PaddingValues.Absolute(),
	drawableKlass: Any? = null
) {
	BoxWithConstraints(Modifier.padding(padding)) {
		val routerState by component.routerState.subscribeAsState()

		val smallestSize = min(maxWidth, maxHeight)
		val isWide = smallestSize >= VERTICAL_CONTROL_WIDTH_THRESHOLD
		FeatureContent(Modifier.fillMaxSize(), routerState, isWide, drawableKlass)
	}
}

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun FeatureContent(
	modifier: Modifier,
	routerState: ChildStack<*, ProjectRoot.Destination>,
	isWide: Boolean,
	drawableKlass: Any? = null
) {
	Children(
		modifier = modifier,
		stack = routerState,
		animation = stackAnimation { _, _, _ -> fade() },
	) {
		when (val child = it.instance) {
			is ProjectRoot.Destination.EditorDestination ->
				ProjectEditorUi(component = child.component, isWide = isWide, drawableKlass = drawableKlass)

			is ProjectRoot.Destination.NotesDestination ->
				NotesUi(child.component)

			is ProjectRoot.Destination.EncyclopediaDestination ->
				EncyclopediaUi(child.component)
		}
	}
}