package com.example.contactapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactapp.R
import com.example.contactapp.data.Contact
import com.example.contactapp.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private val repository: ContactRepository) : ViewModel() {
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.i(this@MainViewModel.javaClass.name, "Start loading contacts")
            try {
                _contacts.value = repository.loadAllContacts()
                Log.i(this@MainViewModel.javaClass.name, "Loading successful")
            } catch (e: Exception) {
                _error.value = R.string.loadingError
                Log.i(
                    this@MainViewModel.javaClass.name,
                    "The load failed with an error:${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}