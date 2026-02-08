package com.h2.wellspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.h2.wellspend.data.AppDatabase
import com.h2.wellspend.data.WellSpendRepository

import com.h2.wellspend.ui.theme.WellSpendTheme
import com.h2.wellspend.ui.viewmodel.UpdateState
import com.h2.wellspend.ui.viewmodel.UpdateViewModel
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import kotlinx.coroutines.runBlocking

import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(this)
        val repository = WellSpendRepository(database)
        
        val (initialThemeMode, initialDynamicColor, initialOnboardingCompleted) = runBlocking {
            val theme = database.settingDao().getSetting("theme_mode")
            val dynamic = database.settingDao().getSetting("dynamic_color")?.toBoolean() ?: true
            var onboarding = database.settingDao().getSetting("onboarding_completed")?.toBoolean() ?: false
            
            if (!onboarding) {
                // Check if user already has accounts (migrating existing user)
                val accounts = database.accountDao().getAllAccountsOneShot()
                if (accounts.isNotEmpty()) {
                    onboarding = true
                    // Persist for future runs
                    database.settingDao().insertSetting(com.h2.wellspend.data.Setting("onboarding_completed", "true"))
                }
            }
            
            Triple(theme, dynamic, onboarding)
        }
        
        val viewModelFactory = MainViewModelFactory(repository, initialThemeMode, initialDynamicColor, initialOnboardingCompleted)
        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]



        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()
            val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()
            val accounts by viewModel.accounts.collectAsState() // Observe accounts for onboarding synchronization


            val darkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            WellSpendTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (onboardingCompleted) {
                        val updateViewModel: UpdateViewModel = viewModel(
                            factory = UpdateViewModel.Factory(applicationContext)
                        )
                        val updateState = updateViewModel.updateState.collectAsState().value

                        LaunchedEffect(Unit) {
                            updateViewModel.checkForUpdates()
                        }

                        MainScreen(viewModel = viewModel, updateViewModel = updateViewModel)
                    } else {
                        com.h2.wellspend.ui.components.OnboardingScreen(
                            onComplete = { viewModel.completeOnboarding() },
                            onThemeChange = { viewModel.updateThemeMode(it) },
                            onDynamicColorChange = { viewModel.updateDynamicColor(it) },
                            onCurrencyChange = { viewModel.updateBudgets(emptyList(), it) }, // Updates global currency
                            onImportData = { uri -> 
                                viewModel.importData(uri, contentResolver) { _, _ -> }
                            },
                            onCreateAccount = { viewModel.addAccount(it) },
                            onCategoriesSelected = { categories ->
                                categories.forEach { 
                                    viewModel.addCategory(it) 
                                }
                            },
                            existingAccounts = accounts, // Pass observed accounts
                            initialTheme = themeMode,
                            initialDynamicColor = dynamicColor
                        )
                    }
                }
            }
        }
    }
}
