package com.h2.wellspend.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Update
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.widget.Toast
import com.h2.wellspend.data.model.GitHubAsset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.h2.wellspend.BuildConfig
import com.h2.wellspend.ui.viewmodel.UpdateState
import com.h2.wellspend.ui.viewmodel.UpdateViewModel

@Composable
fun UpdateScreen(
    viewModel: UpdateViewModel = viewModel()
) {
    val updateState by viewModel.updateState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.updateState.value is UpdateState.Initial) {
            viewModel.checkForUpdates()
        }
    }

    var pendingAsset by remember { mutableStateOf<GitHubAsset?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingAsset != null) {
            viewModel.downloadApk(context, pendingAsset!!.downloadUrl, pendingAsset!!.name)
        } else if (!isGranted) {
             Toast.makeText(context, "Permission required to download on this device", Toast.LENGTH_SHORT).show()
        }
        pendingAsset = null
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        when (val state = updateState) {
            is UpdateState.Initial, is UpdateState.Loading -> {
                CircularProgressIndicator()
                Text(
                    text = "Checking for updates...",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            is UpdateState.Available -> {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Update Available",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Update Available",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Text(
                    text = "Version ${state.release.tagName} is now available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (state.release.assets.isNotEmpty()) {
                    Text(
                        text = "Assets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    state.release.assets.forEach { asset ->
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    pendingAsset = asset
                                    launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    viewModel.downloadApk(context, asset.downloadUrl, asset.name)
                                }
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                            Text("Download ${asset.name}")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                             val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.release.htmlUrl))
                             context.startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        Text("Download Update")
                    }
                }

                androidx.compose.material3.TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.release.htmlUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("View More Details")
                }

                androidx.compose.material3.TextButton(
                    onClick = { viewModel.checkForUpdates(forceCheck = true) },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text("Check Again")
                }

                dev.jeziellago.compose.markdowntext.MarkdownText(
                    markdown = state.release.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start
                    ),
                    modifier = Modifier.padding(vertical = 24.dp)
                )


            }
            is UpdateState.NoUpdate -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Up to Date",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "You're Up to Date!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { viewModel.checkForUpdates(forceCheck = true) },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text("Check Again")
                }
            }
            is UpdateState.Error -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Check Failed",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(onClick = { viewModel.checkForUpdates(forceCheck = true) }) {
                    Text("Retry")
                }
            }

        }
    }
}
