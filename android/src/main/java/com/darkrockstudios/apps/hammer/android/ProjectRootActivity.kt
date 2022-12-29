package com.darkrockstudios.apps.hammer.android

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.decompose.defaultComponentContext
import com.darkrockstudios.apps.hammer.common.AppCloseManager
import com.darkrockstudios.apps.hammer.common.ProjectRootUi
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.data.MenuDescriptor
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.projectroot.ProjectRootComponent

class ProjectRootActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isWide = resources.getBoolean(R.bool.is_wide)

        val projectDef = intent.getParcelableExtra(EXTRA_PROJECT, ProjectDef::class.java)
        if (projectDef == null) {
            finish()
        } else {
            setContent {
                MaterialTheme {
                    val menu = remember { mutableStateOf<Set<MenuDescriptor>>(emptySet()) }
                    val component = remember {
                        ProjectRootComponent(
                            componentContext = defaultComponentContext(),
                            projectDef = projectDef,
                            addMenu = { menuDescriptor ->
                                menu.value =
                                    mutableSetOf(menuDescriptor).apply { add(menuDescriptor) }
                            },
                            removeMenu = { menuId ->
                                menu.value = menu.value.filter { it.id != menuId }.toSet()
                            }
                        )
                    }

                    val scaffoldState = rememberScaffoldState()
                    Scaffold(
                        scaffoldState = scaffoldState,
                        topBar = {
                            TopAppBar(
                                title = { Text("Hammer") },
                                elevation = Ui.ELEVATION,
                                navigationIcon = {
                                    IconButton(onClick = ::onBackPressed) {
                                        Icon(Icons.Filled.ArrowBack, "backIcon")
                                    }
                                },
                                actions = {
                                    if (menu.value.isNotEmpty()) {
                                        TopAppBarDropdownMenu(menu.value.toList())
                                    }
                                }
                            )
                        },
                        content = { padding ->
                            ProjectRootUi(component)

                            // TODO: This needs to be state
                            //val shouldConfirmClose = component.shouldConfirmClose.subscribeAsState()
                            BackHandler(enabled = component.hasUnsavedBuffers()) {
                                confirmCloseDialog(component)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun confirmCloseDialog(component: AppCloseManager) {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Scenes")
            .setMessage("Save unsaved scenes?")
            .setNegativeButton("Discard and close") { _, _ -> finish() }
            .setNeutralButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Save and close") { _, _ ->
                component.storeDirtyBuffers()
                finish()
            }
            .create()
            .show()
    }

    companion object {
        const val EXTRA_PROJECT = "project"
    }
}