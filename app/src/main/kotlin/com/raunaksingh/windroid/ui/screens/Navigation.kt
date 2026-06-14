/*
 * WinDroid - Navigation + Shared Components
 * Created by Raunak Singh
 */

package com.raunaksingh.windroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raunaksingh.windroid.core.*

@Composable
fun MainNavigation(vm: WinDroidViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    AnimatedContent(
        targetState = state.currentScreen,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith
            slideOutHorizontally { -it } + fadeOut()
        }
    ) { screen ->
        when (screen) {
            Screen.Home     -> HomeScreen(state, vm)
            Screen.Tweaks   -> TweaksScreen(state, vm)
            Screen.Progress -> ProgressScreen(state, vm)
            Screen.Done     -> DoneScreen(state, vm)
        }
    }
}
