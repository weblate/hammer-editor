package com.darkrockstudios.apps.hammer.common

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

abstract class ComponentBase(componentContext: ComponentContext) :
    ComponentContext by componentContext, Lifecycle.Callbacks {

    protected val scope = CoroutineScope(defaultDispatcher)

    private fun setup() {
        lifecycle.subscribe(this)
    }

    override fun onDestroy() {
        scope.cancel("${this::class.simpleName} destroyed")
    }

    init {
        setup()
    }
}