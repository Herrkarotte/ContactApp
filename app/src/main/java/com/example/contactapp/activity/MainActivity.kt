package com.example.contactapp.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.contactapp.data.Contact
import com.example.contactapp.ui.theme.ContactAppTheme
import com.example.contactapp.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContactAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ContactApp(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
fun ContactApp(
    modifier: Modifier = Modifier, viewModel: MainViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val contactPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity, Manifest.permission.READ_CONTACTS
                )
                scope.launch {
                    if (!shouldShow) {
                        val result = snackbarHostState.showSnackbar(
                            "Permission to make calls is permanently denied. Please enable this in the application settings, open them?",
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                            actionLabel = "Open Settings"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            "Contact permission denied", duration = SnackbarDuration.Short
                        )
                    }
                }
            } else {
                viewModel.loadContacts()
            }
        }
    LaunchedEffect(Unit) {
        contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
    val callPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity, Manifest.permission.CALL_PHONE
                )
                scope.launch {
                    if (!shouldShow) {
                        val result = snackbarHostState.showSnackbar(
                            "Permission to make calls is permanently denied. Please enable this in the application settings, open them?",
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                            actionLabel = "Open settings"
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            "Call permission denied", duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    val onContactClick = onContactClick@{ phone: String ->
        if (phone.isBlank()) return@onContactClick
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            makeCall(context, phone)
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }
    Box(modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            error != null -> ErrorMessage(error!!)
            else -> GroupedContactList(contacts, modifier, onClick = onContactClick)
        }
        SnackbarHost(
            hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun makeCall(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${phone.filter { it.isDigit() || it == '+' }}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot make a call", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun GroupedContactList(contacts: List<Contact>, modifier: Modifier, onClick: (String) -> Unit) {
    val groupedContacts = remember(contacts) {
        contacts.groupBy { it.name.first().uppercaseChar() }.toSortedMap()
    }

    LazyColumn(modifier = modifier) {
        groupedContacts.forEach { (initial, contactsInGroup) ->
            stickyHeader {
                InitialHeader(initial.toString())
            }
            items(contactsInGroup, key = { it.id }) { contact ->
                ContactCard(contact, { onClick(contact.phone) })
            }
        }
    }
}

@Composable
fun InitialHeader(initial: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 0.dp)
    ) {
        Text(
            text = initial, fontSize = 24.sp
        )
    }
}

@Composable
fun ContactCard(contact: Contact, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .shadow(10.dp, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = contact.name, fontSize = 18.sp
            )
            Text(
                text = contact.phone, fontSize = 18.sp
            )
        }
    }
}

@Composable
fun ErrorMessage(error: String) {
    Text(text = "Ошибка $error")
}