package com.darkrockstudios.apps.hammer.common.encyclopedia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.darkrockstudios.apps.hammer.common.compose.ExposedDropDown
import com.darkrockstudios.apps.hammer.common.compose.ImageItem
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.data.encyclopediarepository.EncyclopediaRepository
import com.darkrockstudios.apps.hammer.common.data.encyclopediarepository.EntryError
import com.darkrockstudios.apps.hammer.common.data.encyclopediarepository.entry.EntryType
import com.darkrockstudios.apps.hammer.common.getHomeDirectory
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateEntryUi(
	component: CreateEntry,
	scope: CoroutineScope,
	snackbarHostState: SnackbarHostState,
	modifier: Modifier,
	close: () -> Unit
) {
	val state by component.state.subscribeAsState()

	var newEntryNameText by remember { mutableStateOf("") }
	var newEntryContentText by remember { mutableStateOf(TextFieldValue("")) }
	var newTagsText by remember { mutableStateOf("") }
	var selectedType by remember { mutableStateOf(EntryType.PERSON) }
	val types = remember { EntryType.values().toList() }

	var showFilePicker by remember { mutableStateOf(false) }
	var imagePath by remember { mutableStateOf<String?>(null) }

	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.Center
	) {
		Card {
			Column(
				modifier = modifier.padding(Ui.Padding.XL)
					.widthIn(128.dp, 420.dp)
					.verticalScroll(rememberScrollState())
			) {

				Text(
					"Create New Entry",
					modifier = Modifier.padding(PaddingValues(bottom = Ui.Padding.XL)),
					style = MaterialTheme.typography.displayMedium
				)

				Text(
					"Type:",
					style = MaterialTheme.typography.headlineSmall,
					modifier = Modifier.padding(bottom = Ui.Padding.M)
				)

				ExposedDropDown(
					modifier = Modifier.fillMaxWidth(),
					padding = Ui.Padding.XL,
					items = types,
					defaultIndex = types.indexOf(EntryType.PERSON)
				) { item ->
					if (item != null) {
						selectedType = item
					} else {
						Napier.w { "EntryType cannot be null" }
					}
				}

				TextField(
					modifier = Modifier.fillMaxWidth()
						.padding(PaddingValues(top = Ui.Padding.XL, bottom = Ui.Padding.XL)),
					value = newEntryNameText,
					onValueChange = { newEntryNameText = it },
					placeholder = { Text("Name") }
				)

				TextField(
					modifier = Modifier.fillMaxWidth().padding(PaddingValues(bottom = Ui.Padding.XL)),
					value = newTagsText,
					onValueChange = { newTagsText = it },
					placeholder = { Text("Tags (space seperated)") }
				)

				OutlinedTextField(
					value = newEntryContentText,
					onValueChange = { newEntryContentText = it },
					modifier = Modifier.fillMaxWidth().padding(PaddingValues(bottom = Ui.Padding.XL)),
					placeholder = { Text(text = "Describe your entry") },
					maxLines = 10,
				)

				Button(onClick = { showFilePicker = true }) {
					Text("Select Image")
				}

				Row(modifier = Modifier.fillMaxWidth()) {
					Button(
						modifier = Modifier.weight(1f).padding(PaddingValues(end = Ui.Padding.XL)),
						onClick = {
							val result = component.createEntry(
								name = newEntryNameText,
								type = selectedType,
								text = newEntryContentText.text,
								tags = newTagsText.splitToSequence(" ").toList(),
								imagePath = imagePath
							)
							when (result.error) {
								EntryError.NAME_TOO_LONG -> scope.launch { snackbarHostState.showSnackbar("Entry Name was too long. Max ${EncyclopediaRepository.MAX_NAME_SIZE}") }
								EntryError.NAME_INVALID_CHARACTERS -> scope.launch { snackbarHostState.showSnackbar("Entry Name must be alpha-numeric") }
								EntryError.TAG_TOO_LONG -> scope.launch { snackbarHostState.showSnackbar("Tag is too long. Max ${EncyclopediaRepository.MAX_TAG_SIZE}") }
								EntryError.NONE -> {
									newEntryNameText = ""
									close()
									scope.launch { snackbarHostState.showSnackbar("Entry Created") }
								}
							}
						}
					) {
						Text("Create")
					}

					Button(
						modifier = Modifier.weight(1f).padding(PaddingValues(start = Ui.Padding.XL)),
						onClick = { close() }
					) {
						Text("Cancel")
					}
				}

				if (imagePath != null) {
					Row {
						ImageItem(
							modifier = Modifier.size(128.dp).background(Color.LightGray),
							path = imagePath
						)
						Button(
							modifier = Modifier.weight(1f).padding(PaddingValues(start = Ui.Padding.XL)),
							onClick = { imagePath = null }
						) {
							Text("Remove Image")
						}
					}
				}
			}
		}
	}

	FilePicker(
		show = showFilePicker,
		fileExtension = "jpg",
		initialDirectory = getHomeDirectory()
	) { path ->
		imagePath = path
		showFilePicker = false
	}
}