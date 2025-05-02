package com.example.contactapp.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.example.contactapp.data.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContactRepository @Inject constructor(private val context: Context) {
    suspend fun loadAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver
        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID).takeIf { it >= 0 }
            val nameIndex =
                cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME).takeIf { it >= 0 }
            val phoneIndex =
                cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER).takeIf { it >= 0 }
            if (idIndex == null || nameIndex == null || phoneIndex == null) {
                return@withContext emptyList()
            }
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex) ?: "Без имени"
                val phone = if (cursor.getInt(phoneIndex) > 0) {
                    getFirstPhone(contentResolver, id) ?: ""
                } else {
                    ""
                }
                contactsList.add(Contact(id, name, phone))
            }
        }
        return@withContext contactsList
    }

    private fun getFirstPhone(
        contentResolver: ContentResolver, contactId: String
    ): String? {
        return contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}= ? AND " + "${ContactsContract.CommonDataKinds.Phone.TYPE} = ?",
            arrayOf(contactId, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    .takeIf { it >= 0 }
                if (index == null) {
                    null
                } else {
                    cursor.getString(index)
                }
            } else {
                null
            }
        }
    }
}