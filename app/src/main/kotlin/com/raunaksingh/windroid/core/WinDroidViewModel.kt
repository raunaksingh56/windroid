/*
 * WinDroid - Main ViewModel
 * Created by Raunak Singh
 */

package com.raunaksingh.windroid.core

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.raunaksingh.windroid.tweaks.AutounattendGenerator
import com.raunaksingh.windroid.tweaks.TweakConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FlashState(
    val isoUri: Uri?        = null,
    val isoName: String     = "",
    val isoSizeMb: Long     = 0,
    val usbUri: Uri?        = null,
    val usbLabel: String    = "",
    val tweakConfig: TweakConfig = TweakConfig(),
    val isFlashing: Boolean = false,
    val flashProgress: Int  = 0,
    val flashMessage: String= "",
    val logLines: List<String> = emptyList(),
    val isSuccess: Boolean  = false,
    val errorMessage: String? = null,
    val currentScreen: Screen = Screen.Home,
    val bootLevel: BootLevel? = null,
    val bootLevelMessage: String = ""
)

enum class Screen { Home, Tweaks, Progress, Done }

class WinDroidViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(FlashState())
    val state: StateFlow<FlashState> = _state.asStateFlow()

    private val usbWriter = UsbWriter(app)

    fun selectIso(uri: Uri, name: String, sizeMb: Long) {
        _state.update { it.copy(isoUri = uri, isoName = name, isoSizeMb = sizeMb) }
    }

    fun selectUsb(uri: Uri, label: String) {
        _state.update { it.copy(usbUri = uri, usbLabel = label) }
    }

    fun updateTweaks(config: TweakConfig) {
        _state.update { it.copy(tweakConfig = config) }
    }

    fun navigateTo(screen: Screen) {
        _state.update { it.copy(currentScreen = screen) }
    }

    fun startFlash() {
        val s = _state.value
        if (s.isoUri == null || s.usbUri == null) return

        _state.update { it.copy(
            isFlashing = true,
            flashProgress = 0,
            flashMessage = "Preparing...",
            logLines = emptyList(),
            isSuccess = false,
            errorMessage = null,
            currentScreen = Screen.Progress
        )}

        val xml = AutounattendGenerator.generate(s.tweakConfig)

        viewModelScope.launch(Dispatchers.IO) {
            usbWriter.flashToUsb(
                isoUri          = s.isoUri,
                usbUri          = s.usbUri,
                autounattendXml = xml
            ) { event ->
                when (event) {
                    is FlashEvent.Progress -> _state.update { it.copy(
                        flashProgress = event.percent,
                        flashMessage  = event.message
                    )}
                    is FlashEvent.Log -> _state.update { it.copy(
                        logLines = it.logLines + event.message
                    )}
                    is FlashEvent.BootLevelDetected -> _state.update { it.copy(
                        bootLevel        = event.level,
                        bootLevelMessage = event.message
                    )}
                    is FlashEvent.Success -> _state.update { it.copy(
                        isFlashing    = false,
                        isSuccess     = true,
                        currentScreen = Screen.Done
                    )}
                    is FlashEvent.Error -> _state.update { it.copy(
                        isFlashing    = false,
                        errorMessage  = event.message
                    )}
                }
            }
        }
    }

    fun reset() {
        _state.value = FlashState()
    }
}
