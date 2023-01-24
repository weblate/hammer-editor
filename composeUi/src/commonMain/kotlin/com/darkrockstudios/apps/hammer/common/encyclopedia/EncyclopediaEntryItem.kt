package com.darkrockstudios.apps.hammer.common.encyclopedia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.darkrockstudios.apps.hammer.common.compose.ImageItem
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.data.encyclopediarepository.entry.EntryContent
import com.darkrockstudios.apps.hammer.common.data.encyclopediarepository.entry.EntryDef
import com.darkrockstudios.apps.hammer.common.mainDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EncyclopediaEntryItem(
	entryDef: EntryDef,
	component: BrowseEntries,
	viewEntry: (EntryDef) -> Unit,
	scope: CoroutineScope,
	modifier: Modifier = Modifier,
) {
	var loadContentJob by remember { mutableStateOf<Job?>(null) }
	var entryContent by remember { mutableStateOf<EntryContent?>(null) }
	var entryImagePath by remember { mutableStateOf<String?>(null) }

	LaunchedEffect(entryDef) {
		entryImagePath = null
		loadContentJob?.cancel()
		loadContentJob = scope.launch {
			entryImagePath = component.getImagePath(entryDef)
			val content = component.loadEntryContent(entryDef)
			withContext(mainDispatcher) {
				entryContent = content
				loadContentJob = null
			}
		}
	}

	Card(
		modifier = modifier
			.fillMaxWidth()
			.padding(Ui.Padding.XL)
			.clickable { viewEntry(entryDef) },
	) {
		Column {

			if (entryImagePath != null) {
				ImageItem(
					path = entryImagePath,
					modifier = Modifier.fillMaxWidth().heightIn(64.dp, 256.dp),
					contentScale = ContentScale.FillWidth
				)
			}

			Text(entryDef.id.toString())
			Text(entryDef.type.text)
			Text(entryDef.name)

			if (loadContentJob != null) {
				CircularProgressIndicator()
			} else {
				val content = entryContent
				if (content != null) {
					Text("Content: " + content.text)

					Row {
						content.tags.forEach { tag ->
							Text(tag)
						}
					}
				} else {
					Text("Error: Failed to load entry!")
				}
			}
		}
	}
}