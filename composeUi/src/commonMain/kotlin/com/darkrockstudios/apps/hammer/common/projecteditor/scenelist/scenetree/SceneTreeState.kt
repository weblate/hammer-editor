package com.darkrockstudios.apps.hammer.common.projecteditor.scenelist.scenetree

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.darkrockstudios.apps.hammer.common.data.InsertPosition
import com.darkrockstudios.apps.hammer.common.data.MoveRequest
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.data.SceneSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val collapsedNotesSaver = Saver<SnapshotStateMap<Int, Boolean>, List<Pair<Int, Boolean>>>(
	save = { map ->
		map.toList()
	},
	restore = { saved ->
		val map = SnapshotStateMap<Int, Boolean>()
		saved.forEach { item ->
			map[item.first] = item.second
		}
		map
	}
)

@Composable
fun rememberReorderableLazyListState(
	summary: SceneSummary,
	moveItem: (moveRequest: MoveRequest) -> Unit,
): SceneTreeState {
	val coroutineScope = rememberCoroutineScope()
	val listState = rememberLazyListState()
	val collapsedNodes = rememberSaveable(saver = collapsedNotesSaver) { mutableStateMapOf() }

	return remember {
		SceneTreeState(
			sceneSummary = summary,
			moveItem = moveItem,
			coroutineScope = coroutineScope,
			listState = listState,
			collapsedNodes = collapsedNodes
		)
	}
}

class SceneTreeState(
	sceneSummary: SceneSummary,
	val moveItem: (moveRequest: MoveRequest) -> Unit,
	val coroutineScope: CoroutineScope,
	val listState: LazyListState,
	val collapsedNodes: SnapshotStateMap<Int, Boolean>,
) {
	internal var summary by mutableStateOf(sceneSummary)
	var selectedId by mutableStateOf(NO_SELECTION)
	var insertAt by mutableStateOf<InsertPosition?>(null)

	private var scrollJob by mutableStateOf<Job?>(null)
	private var treeHash by mutableStateOf(sceneSummary.sceneTree.hashCode())

	fun getTree() = summary.sceneTree

	fun updateSummary(sceneSummary: SceneSummary) {
		if (summary != sceneSummary) {
			summary = sceneSummary
			cleanUpOnDelete()
		}
	}

	private fun cleanUpOnDelete() {
		val newHash = summary.sceneTree.hashCode()
		if (treeHash != newHash) {
			treeHash = newHash

			// Prune layouts if the id is not found in the tree
			val nodeIt = collapsedNodes.iterator()
			while (nodeIt.hasNext()) {
				val (id, _) = nodeIt.next()
				val foundNode = summary.sceneTree.findBy { it.id == id }
				if (foundNode == null) {
					nodeIt.remove()
				}
			}
		}
	}

	fun collapseAll() {
		summary.sceneTree
			.filter { it.value.type == SceneItem.Type.Group }
			.forEach { node ->
				collapsedNodes[node.value.id] = true
			}
	}

	fun expandAll() {
		collapsedNodes.clear()
	}

	fun autoScroll(up: Boolean) {
		val previousIndex = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
		if (scrollJob?.isActive != true) {
			scrollJob = if (up) {
				if (previousIndex > 0) {
					coroutineScope.launch {
						listState.animateScrollToItem(previousIndex)
					}
				} else {
					null
				}
			} else {
				coroutineScope.launch {
					listState.layoutInfo.apply {
						val viewportHeight = viewportEndOffset + viewportStartOffset
						val index = visibleItemsInfo.size + previousIndex
						val lastInfo = visibleItemsInfo[visibleItemsInfo.size - 1]
						val offset = lastInfo.size - viewportHeight

						listState.animateScrollToItem(index, offset)
					}
				}
			}
		}
	}

	fun startDragging(id: Int) {
		if (selectedId == NO_SELECTION) {
			selectedId = id
		}
	}

	fun stopDragging() {
		val insertPosition = insertAt
		val selectedIndex = summary.sceneTree.indexOf { it.id == selectedId }
		if (selectedIndex > 0 && insertPosition != null) {
			val request = MoveRequest(
				selectedId,
				insertPosition
			)
			moveItem(request)
		}

		selectedId = NO_SELECTION
		insertAt = null
	}

	fun toggleExpanded(nodeId: Int) {
		val collapse = !(collapsedNodes[nodeId] ?: false)
		collapsedNodes[nodeId] = collapse
	}

	companion object {
		const val NO_SELECTION = -1
	}
}
