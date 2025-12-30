package com.h2.wellspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.h2.wellspend.data.AppDatabase
import com.h2.wellspend.data.WellSpendRepository
import com.h2.wellspend.ui.theme.WellSpendTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = WellSpendRepository(database)
        
        var initialThemeMode: String? = null
        var initialDynamicColor: Boolean = false
        
        runBlocking {
            initialThemeMode = database.settingDao().getSetting("theme_mode")
            initialDynamicColor = database.settingDao().getSetting("dynamic_color")?.toBoolean() ?: false
        }
        
        val viewModelFactory = MainViewModelFactory(repository, initialThemeMode, initialDynamicColor)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]



        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()

            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            WellSpendTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
