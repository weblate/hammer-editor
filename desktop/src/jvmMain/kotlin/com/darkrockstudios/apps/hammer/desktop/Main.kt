package com.darkrockstudios.apps.hammer.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.application
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.getAndUpdate
import com.darkrockstudios.apps.hammer.common.AppCloseManager
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.compose.getDefaultDispatcher
import com.darkrockstudios.apps.hammer.common.compose.getMainDispatcher
import com.darkrockstudios.apps.hammer.common.compose.moko.get
import com.darkrockstudios.apps.hammer.common.compose.theme.AppTheme
import com.darkrockstudios.apps.hammer.common.data.globalsettings.GlobalSettingsRepository
import com.darkrockstudios.apps.hammer.common.data.globalsettings.UiTheme
import com.darkrockstudios.apps.hammer.common.dependencyinjection.NapierLogger
import com.darkrockstudios.apps.hammer.common.dependencyinjection.imageLoadingModule
import com.darkrockstudios.apps.hammer.common.dependencyinjection.mainModule
import com.darkrockstudios.apps.hammer.common.getDataVersion
import com.darkrockstudios.apps.hammer.common.setDataVersion
import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.DarculaTheme
import com.github.weisj.darklaf.theme.IntelliJTheme
import com.jthemedetecor.OsThemeDetector
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.LocalImageLoader
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.getKoin

private fun handleArguments(args: Array<String>) {
	val parser = ArgParser("hammer")

	val dataVersion by parser.option(
		ArgType.String,
		shortName = "d",
		fullName = "dataVersion",
		description = "Data version"
	).default(getDataVersion())

	parser.parse(args)

	setDataVersion(dataVersion)
}

@ExperimentalDecomposeApi
@ExperimentalMaterialApi
@ExperimentalComposeApi
fun main(args: Array<String>) {
	handleArguments(args)

	Napier.base(DebugAntilog())

	GlobalContext.startKoin {
		logger(NapierLogger())
		modules(mainModule, imageLoadingModule)
	}

	val osThemeDetector = OsThemeDetector.getDetector()
	if (osThemeDetector.isDark) {
		LafManager.install(DarculaTheme())
	} else {
		LafManager.install(IntelliJTheme())
	}

	val scope = CoroutineScope(getDefaultDispatcher())
	val mainDispatcher = getMainDispatcher()

	// Listen and react to Global Settings updates
	val globalSettingsRepository = getKoin().get<GlobalSettingsRepository>()
	val globalSettings = MutableValue(globalSettingsRepository.globalSettings)
	val settingsUpdateJob = scope.launch {
		globalSettingsRepository.globalSettingsUpdates.collect { settings ->
			withContext(mainDispatcher) {
				globalSettings.getAndUpdate { settings }
			}
		}
	}

	application {
		val applicationState = remember { ApplicationState() }
		val imageLoader: ImageLoader = get(ImageLoader::class.java)

		val settingsState by globalSettings.subscribeAsState()
		val initialDark = when (settingsState.uiTheme) {
			UiTheme.Light -> false
			UiTheme.Dark -> true
			UiTheme.FollowSystem -> osThemeDetector.isDark
		}
		var darkMode by remember(initialDark) { mutableStateOf(initialDark) }
		val themeListener = remember {
			{ isDarkModeEnabled: Boolean ->
				darkMode = isDarkModeEnabled

				if (darkMode) {
					LafManager.install(DarculaTheme())
				} else {
					LafManager.install(IntelliJTheme())
				}
				LafManager.updateLaf()
			}
		}
		if (settingsState.uiTheme == UiTheme.FollowSystem) {
			osThemeDetector.registerListener(themeListener)
		} else {
			osThemeDetector.registerListener(themeListener)
		}

		AppTheme(useDarkTheme = darkMode) {
			CompositionLocalProvider(
				LocalImageLoader provides imageLoader,
			) {
				when (val windowState = applicationState.windows.value) {
					is WindowState.ProjectSectionWindow -> {
						ProjectSelectionWindow { project ->
							applicationState.openProject(project)
						}
					}

					is WindowState.ProjectWindow -> {
						ProjectEditorWindow(applicationState, windowState.projectDef)
					}
				}
			}
		}
	}

	settingsUpdateJob.cancel()
}

@ExperimentalMaterialApi
@ExperimentalComposeApi
@Composable
internal fun confirmCloseDialog(
	closeType: ApplicationState.CloseType,
	dismissDialog: (ConfirmCloseResult, ApplicationState.CloseType) -> Unit
) {
	AlertDialog(
		title = { Text(DR.strings.unsaved_scenes_dialog_title.get()) },
		text = { Text(DR.strings.unsaved_scenes_dialog_message.get()) },
		onDismissRequest = { /* Noop */ },
		buttons = {
			Column(
				modifier = Modifier.fillMaxWidth(),
			) {
				Button(onClick = { dismissDialog(ConfirmCloseResult.SaveAll, closeType) }) {
					Text(DR.strings.unsaved_scenes_dialog_positive_button.get())
				}
				Button(onClick = {
					dismissDialog(
						ConfirmCloseResult.Cancel,
						ApplicationState.CloseType.None
					)
				}) {
					Text(DR.strings.unsaved_scenes_dialog_neutral_button.get())
				}
				Button(onClick = { dismissDialog(ConfirmCloseResult.Discard, closeType) }) {
					Text(DR.strings.unsaved_scenes_dialog_negative_button.get())
				}
			}
		},
		modifier = Modifier.width(300.dp).padding(Ui.Padding.XL)
	)
}

internal enum class ConfirmCloseResult {
	SaveAll,
	Discard,
	Cancel
}

internal fun ApplicationScope.performClose(
	app: ApplicationState,
	closeType: ApplicationState.CloseType
) {
	when (closeType) {
		ApplicationState.CloseType.Application -> exitApplication()
		ApplicationState.CloseType.Project -> app.closeProject()
		ApplicationState.CloseType.None -> {
			/* noop */
		}
	}
}

internal fun ApplicationScope.onRequestClose(
	component: AppCloseManager,
	app: ApplicationState,
	closeType: ApplicationState.CloseType
) {
	if (component.hasUnsavedBuffers()) {
		app.showConfirmProjectClose(closeType)
	} else {
		performClose(app, closeType)
	}
}
