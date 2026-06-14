/*
 * WinDroid - Bootable Windows USB Creator for Android
 * Created by Raunak Singh
 * GitHub: https://github.com/raunaksingh
 * License: GPL-3.0
 */

package com.raunaksingh.windroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.raunaksingh.windroid.ui.theme.WinDroidTheme
import com.raunaksingh.windroid.ui.screens.MainNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WinDroidTheme {
                MainNavigation()
            }
        }
    }
}
