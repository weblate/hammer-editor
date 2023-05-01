package com.darkrockstudios.apps.hammer.common.projecteditor.scenelist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.darkrockstudios.apps.hammer.common.compose.MpDialog
import com.darkrockstudios.apps.hammer.common.compose.Ui

@ExperimentalMaterial3Api
@Composable
internal fun CreateDialog(
	show: Boolean,
	title: String,
	textLabel: String,
	onClose: (name: String?) -> Unit
) {
	var nameText by rememberSaveable { mutableStateOf("") }
	fun close(text: String?) {
		onClose(text)
		nameText = ""
	}

	MpDialog(
		visible = show,
		title = title,
		onCloseRequest = { close(null) }
	) {
		Box(modifier = Modifier.fillMaxWidth()) {
			Column(
				modifier = Modifier
					.width(IntrinsicSize.Max)
					.align(Alignment.Center)
			) {
				TextField(
					value = nameText,
					onValueChange = { nameText = it },
					label = { Text(textLabel) }
				)

				Spacer(modifier = Modifier.size(Ui.Padding.XL))

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween
				) {
					Button(onClick = { close(nameText) }) {
						Text("Create")
					}

					Button(onClick = { close(null) }) {
						Text("Cancel")
					}
				}
			}
		}
	}
}