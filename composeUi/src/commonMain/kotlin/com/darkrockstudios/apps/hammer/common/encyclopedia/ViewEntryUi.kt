package com.darkrockstudios.apps.hammer.common.encyclopedia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.darkrockstudios.apps.hammer.common.components.encyclopedia.ViewEntry
import com.darkrockstudios.apps.hammer.common.compose.*
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewEntryUi(
	component: ViewEntry,
	scope: CoroutineScope,
	modifier: Modifier = Modifier,
	snackbarHostState: SnackbarHostState,
	closeEntry: () -> Unit,
) {
	val dispatcherMain = rememberMainDispatcher()
	val dispatcherDefault = rememberDefaultDispatcher()
	val state by component.state.subscribeAsState()

	var editName by rememberSaveable { mutableStateOf(false) }
	var entryNameText by rememberSaveable { mutableStateOf(state.content?.name ?: "") }

	var editText by rememberSaveable { mutableStateOf(false) }
	var entryText by rememberSaveable { mutableStateOf(state.content?.text ?: "") }

	var discardConfirm by rememberSaveable { mutableStateOf(false) }
	var closeConfirm by rememberSaveable { mutableStateOf(false) }

	val screen = LocalScreenCharacteristic.current
	val content = state.content

	LaunchedEffect(state.content) {
		state.content?.let {
			entryNameText = it.name
			entryText = it.text
		}
	}

	Box(
		modifier = modifier.fillMaxSize().padding(Ui.Padding.XL),
		contentAlignment = Alignment.TopCenter
	) {
		Column(modifier = Modifier.widthIn(128.dp, 700.dp).wrapContentHeight()) {
			Row(
				modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				if (content != null && (editName || editText)) {
					IconButton(onClick = {
						scope.launch {
							component.updateEntry(
								name = entryNameText,
								text = entryText,
								tags = content.tags
							)

							withContext(dispatcherMain) {
								editName = false
								editText = false
							}

							scope.launch {
								snackbarHostState.showSnackbar("Entry Saved")
							}
						}
					}) {
						Icon(
							Icons.Filled.Check,
							"Save",
							tint = MaterialTheme.colorScheme.onSurface
						)
					}

					IconButton(onClick = { discardConfirm = true }) {
						Icon(
							Icons.Filled.Cancel,
							"Cancel",
							tint = MaterialTheme.colorScheme.error
						)
					}

					if (screen.needsExplicitClose) {
						Spacer(modifier = Modifier.size(Ui.Padding.XL))

						Divider(
							color = MaterialTheme.colorScheme.outline,
							modifier = Modifier.fillMaxHeight().width(1.dp)
								.padding(top = Ui.Padding.M, bottom = Ui.Padding.M)
						)

						Spacer(modifier = Modifier.size(Ui.Padding.XL))
					}

					if (discardConfirm) {
						SimpleConfirm(
							title = "Discard Changes?",
							message = "You will lose any changes you have made.",
							onDismiss = { discardConfirm = false }
						) {
							entryNameText = content.name
							entryText = content.text

							editName = false
							editText = false

							discardConfirm = false
						}
					}
				}

				if (screen.needsExplicitClose) {
					IconButton(
						onClick = {
							if (editName || editText) {
								closeConfirm = true
							} else {
								closeEntry()
							}
						},
					) {
						Icon(
							Icons.Filled.Close,
							contentDescription = "Close Entry",
							tint = MaterialTheme.colorScheme.onSurface
						)
					}
				}
			}

			if (editName) {
				TextField(
					modifier = Modifier.wrapContentHeight().fillMaxWidth(),
					value = entryNameText,
					onValueChange = { entryNameText = it },
					placeholder = { Text("Name") }
				)
			} else {
				Text(
					entryNameText,
					style = MaterialTheme.typography.displayMedium,
					color = MaterialTheme.colorScheme.onBackground,
					textAlign = TextAlign.Center,
					modifier = Modifier.wrapContentHeight().fillMaxWidth().clickable { editName = true }
				)
			}

			Spacer(modifier = Modifier.size(Ui.Padding.L))

			if (screen.isWide) {
				Row {
					Image(
						modifier = Modifier.weight(1f),
						state = state,
						showDeleteImageDialog = component::showDeleteImageDialog
					)
					Contents(
						modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
						state = state,
						editText = editText,
						entryText = entryText,
						setEntryText = { entryText = it },
						beginEdit = { editText = true }
					)
				}
			} else {
				Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
					Image(
						modifier = Modifier.fillMaxWidth().wrapContentHeight(),
						state = state,
						showDeleteImageDialog = component::showDeleteImageDialog
					)
					Contents(
						modifier = Modifier.wrapContentHeight(),
						state = state,
						editText = editText,
						entryText = entryText,
						setEntryText = { entryText = it },
						beginEdit = { editText = true }
					)
				}
			}
		}
	}

	FilePicker(show = state.showAddImageDialog, fileExtension = "jpg") { path ->
		if (path != null) {
			scope.launch { component.setImage(path) }
		}
		component.closeAddImageDialog()
	}

	if (state.showDeleteImageDialog) {
		SimpleConfirm(
			title = "Delete Image?",
			message = "This cannot be undone!",
			onDismiss = { component.closeDeleteImageDialog() }
		) {
			scope.launch { component.removeEntryImage() }
			component.closeDeleteImageDialog()
		}
	}

	if (state.showDeleteEntryDialog) {
		SimpleConfirm(
			title = "Delete Entry?",
			message = "This cannot be undone!",
			onDismiss = { component.closeDeleteEntryDialog() }
		) {
			scope.launch(dispatcherDefault) {
				if (component.deleteEntry(state.entryDef)) {
					withContext(dispatcherMain) {
						closeEntry()
					}
					snackbarHostState.showSnackbar("Entry Deleted")
				}
			}
			component.closeDeleteEntryDialog()
		}
	}

	if (closeConfirm) {
		SimpleConfirm(
			title = "Discard Changes?",
			message = "You will lose any changes you have made.",
			onDismiss = { closeConfirm = false }
		) {
			closeConfirm = false
			closeEntry()
		}
	}
}

@Composable
private fun Image(
	modifier: Modifier = Modifier,
	state: ViewEntry.State,
	showDeleteImageDialog: () -> Unit
) {
	if (state.entryImagePath != null) {
		Box(modifier = modifier.wrapContentHeight()) {
			ImageItem(
				path = state.entryImagePath,
				modifier = Modifier.wrapContentHeight()
					.fillMaxWidth()
					.align(Alignment.TopEnd)
					.clickable(onClick = showDeleteImageDialog),
				contentScale = ContentScale.FillWidth,
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Contents(
	modifier: Modifier = Modifier,
	state: ViewEntry.State,
	editText: Boolean,
	entryText: String,
	setEntryText: (String) -> Unit,
	beginEdit: () -> Unit,
) {
	val content = state.content

	Column(
		modifier = modifier
			.wrapContentHeight()
			.padding(start = Ui.Padding.XL, end = Ui.Padding.XL, bottom = Ui.Padding.XL)
	) {
		AssistChip(
			onClick = {},
			enabled = false,
			label = { Text(state.entryDef.type.text) },
			leadingIcon = { Icon(getEntryTypeIcon(state.entryDef.type), state.entryDef.type.text) },
			modifier = Modifier.padding(end = Ui.Padding.L)
		)

		Spacer(modifier = Modifier.size(Ui.Padding.XL))

		if (content != null) {
			Column {
				LaunchedEffect(entryText) {
					if (entryText.isBlank()) {
						beginEdit()
					}
				}

				if (editText) {
					OutlinedTextField(
						value = entryText,
						onValueChange = setEntryText,
						modifier = Modifier.fillMaxWidth().padding(PaddingValues(bottom = Ui.Padding.XL)),
						placeholder = { Text(text = "Describe your entry") },
						maxLines = 10,
					)
				} else {
					val text = entryText.ifBlank {
						"Click to enter description"
					}
					Text(
						text,
						modifier = Modifier.fillMaxWidth().clickable { beginEdit() },
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onBackground,
					)
				}

				Spacer(modifier = Modifier.size(Ui.Padding.XL))

				TagRow(
					tags = content.tags,
					modifier = Modifier.fillMaxWidth(),
					alignment = Alignment.End
				)
			}
		} else {
			CircularProgressIndicator()
		}
	}
}