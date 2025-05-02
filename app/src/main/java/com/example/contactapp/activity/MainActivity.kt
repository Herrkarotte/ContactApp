package com.example.contactapp.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                        modifier = Modifier.padding(innerPadding)
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
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.loadContacts()
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Permission denied", duration = SnackbarDuration.Short
                    )
                }
            }
        }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
    }
    Box(modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator()
            error != null -> ErrorMessage(error!!)
            else -> GroupedContactList(contacts, modifier)
        }
    }
}

@Composable
fun GroupedContactList(contacts: List<Contact>, modifier: Modifier) {
    val groupedContacts = remember(contacts) {
        contacts.groupBy { it.name.first().uppercaseChar() }.toSortedMap()
    }

    LazyColumn(modifier = modifier) {
        groupedContacts.forEach { (initial, contactsInGroup) ->
            stickyHeader {
                InitialHeader(initial.toString())
            }
            items(contactsInGroup, key = { it.id }) { contact ->
                ContactCard(contact)
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
fun ContactCard(contact: Contact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .shadow(10.dp, shape = RoundedCornerShape(10.dp))
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