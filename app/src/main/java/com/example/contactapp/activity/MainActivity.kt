package com.example.contactapp.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.contactapp.R
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

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val snackBarContactPermissionMessage =
        stringResource(R.string.snackBarMessageForContactPermission)
    val actionLabel = stringResource(R.string.snackBarActionLabel)
    val shortSnackBarContactPermissionMessage =
        stringResource(R.string.shortSnackBarMessageForContactPermission)

    val contactPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity, Manifest.permission.READ_CONTACTS
                )
                scope.launch {
                    if (!shouldShow) {
                        val result = snackBarHostState.showSnackbar(
                            snackBarContactPermissionMessage,
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                            actionLabel = actionLabel
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        }
                    } else {
                        snackBarHostState.showSnackbar(
                            shortSnackBarContactPermissionMessage, duration = SnackbarDuration.Short
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

    val snackBarCallPermissionMessage = stringResource(R.string.snackBarMessageForCallPermission)
    val shortSnackBarCallPermissionMessage =
        stringResource(R.string.shortSnackBarMessageForCallPermission)

    val callPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity, Manifest.permission.CALL_PHONE
                )
                scope.launch {
                    if (!shouldShow) {
                        val result = snackBarHostState.showSnackbar(
                            snackBarCallPermissionMessage,
                            withDismissAction = true,
                            duration = SnackbarDuration.Long,
                            actionLabel = actionLabel
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            context.startActivity(intent)
                        }
                    } else {
                        snackBarHostState.showSnackbar(
                            shortSnackBarCallPermissionMessage, duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }

    val snackBarCallErrorMessage = stringResource(R.string.errorCall)

    val onContactClick = onContactClick@{ phone: String ->
        if (phone.isBlank()) return@onContactClick
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val isSuccess = makeCall(context, phone)
            if (!isSuccess) {
                scope.launch {
                    snackBarHostState.showSnackbar(
                        snackBarCallErrorMessage, duration = SnackbarDuration.Short
                    )
                }
            }
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }
    Box(modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            error != null -> error?.let { ErrorMessage(it) }
            else -> GroupedContactList(contacts, modifier, onClick = onContactClick)
        }
        SnackbarHost(
            hostState = snackBarHostState, modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun makeCall(context: Context, phone: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = "tel:${phone.filter { it.isDigit() || it == '+' }}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun GroupedContactList(contacts: List<Contact>, modifier: Modifier, onClick: (String) -> Unit) {
    val defaultInitial = stringResource(R.string.NoNameText).first().uppercaseChar()
    val groupedContacts = remember(contacts) {
        contacts.groupBy { contact ->
            contact.name?.firstOrNull()?.uppercaseChar() ?: defaultInitial

        }.toSortedMap()
    }

    LazyColumn(modifier = modifier) {
        groupedContacts.forEach { (initial, contactsInGroup) ->
            stickyHeader {
                InitialHeader(initial.toString())
            }
            items(contactsInGroup, key = { it.id }) { contact ->
                ContactCard(contact) { contact.phone?.let { phone -> onClick(phone) } }
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
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.onPrimary)

            ) {
                Text(
                    text = (contact.name ?: stringResource(R.string.NoNameText))
                        .take(1).uppercase(),
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = contact.name ?: stringResource(R.string.NoNameText), fontSize = 18.sp
                )
                Text(
                    text = contact.phone ?: stringResource(R.string.NoNumberText), fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(error: Int) {
    Text(text = stringResource(error))
}