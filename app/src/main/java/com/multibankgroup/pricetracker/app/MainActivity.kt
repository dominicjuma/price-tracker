package com.multibankgroup.pricetracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.multibankgroup.pricetracker.app.navigation.PriceTrackerNavGraph
import com.multibankgroup.pricetracker.feature.shared_ui.theme.PriceTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single Activity — hosts theme + NavGraph. All logic lives in ViewModels. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PriceTrackerTheme {
                PriceTrackerNavGraph()
            }
        }
    }
}