package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.navigation.GoluMainShell
import com.example.presentation.viewmodel.GoluFeaturesViewModel
import com.example.presentation.viewmodel.GoluV2ViewModel
import com.example.presentation.viewmodel.GoluViewModel
import com.example.ui.theme.GoluTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GoluApplication
        setContent {
            val goluViewModel: GoluViewModel =
                viewModel(
                    factory = GoluViewModel.Factory(applicationContext, app.container)
                )
            val featuresViewModel: GoluFeaturesViewModel =
                viewModel(
                    factory = GoluFeaturesViewModel.Factory(applicationContext, app.container)
                )
            val v2ViewModel: GoluV2ViewModel =
                viewModel(
                    factory = GoluV2ViewModel.Factory(applicationContext, app.container)
                )
            val settings by goluViewModel.settings.collectAsStateWithLifecycle()
            GoluTheme(darkTheme = settings.isDarkTheme) {
                GoluMainShell(
                    viewModel = goluViewModel,
                    featuresViewModel = featuresViewModel,
                    v2ViewModel = v2ViewModel,
                )
            }
        }
    }
}
