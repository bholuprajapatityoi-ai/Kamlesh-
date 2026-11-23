package com.example.domain.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.remote.dto.ContactAliasDto
import com.example.domain.model.ContactLookupResult
import com.example.domain.model.ContactMatch

class ContactLookupHelper(private val context: Context) {

    fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun lookupByQuery(
        rawQuery: String,
        aliases: List<ContactAliasDto> = emptyList(),
    ): ContactLookupResult {
        val cleaned = normalizeQuery(rawQuery)
        if (cleaned.isBlank()) return ContactLookupResult.NotFound

        // 1. Check user-configured nickname aliases (e.g. Papa, Mummy, Brother, Sister)
        val matchedAlias =
            aliases.firstOrNull { it.nickname.equals(cleaned, ignoreCase = true) }
                ?: aliases.firstOrNull { cleaned.contains(it.nickname.lowercase()) }

        val targetName = matchedAlias?.contactName ?: defaultRelationshipName(cleaned) ?: cleaned

        // If user already supplied a phone hint with alias and Contacts permission is not granted yet
        if (!hasContactsPermission()) {
            val hintDigits = matchedAlias?.phoneHint?.filter { it.isDigit() || it == '+' }.orEmpty()
            return if (hintDigits.length >= 6) {
                ContactLookupResult.Single(
                    ContactMatch(
                        displayName = matchedAlias?.contactName ?: cleaned,
                        phoneNumber = hintDigits,
                    )
                )
            } else {
                ContactLookupResult.PermissionRequired
            }
        }

        val matches = queryDeviceContacts(targetName, cleaned)
        return when {
            matches.isEmpty() -> {
                val hintDigits = matchedAlias?.phoneHint?.filter { it.isDigit() || it == '+' }.orEmpty()
                if (hintDigits.length >= 6) {
                    ContactLookupResult.Single(
                        ContactMatch(
                            displayName = matchedAlias?.contactName ?: targetName,
                            phoneNumber = hintDigits,
                        )
                    )
                } else {
                    ContactLookupResult.NotFound
                }
            }
            matches.size == 1 -> ContactLookupResult.Single(matches.first())
            else -> ContactLookupResult.Multiple(matches.take(5))
        }
    }

    private fun queryDeviceContacts(primaryName: String, fallbackQuery: String): List<ContactMatch> {
        val found = LinkedHashMap<String, ContactMatch>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty().trim() else ""
                    val num = if (numIdx >= 0) cursor.getString(numIdx).orEmpty().trim() else ""
                    if (name.isBlank() || num.isBlank()) continue
                    val lower = name.lowercase()
                    if (lower.contains(primaryName.lowercase()) ||
                        lower.contains(fallbackQuery.lowercase())
                    ) {
                        val normalizedPhone = num.filter { it.isDigit() || it == '+' }
                        if (normalizedPhone.isNotBlank()) {
                            found.putIfAbsent("$lower|$normalizedPhone", ContactMatch(name, normalizedPhone))
                        }
                    }
                }
            }
        }
        return found.values.toList()
    }

    private fun defaultRelationshipName(q: String): String? =
        when {
            q.contains("papa") || q.contains("dad") || q.contains("father") -> "Papa"
            q.contains("mummy") || q.contains("mom") || q.contains("maa") -> "Mummy"
            q.contains("brother") || q.contains("bhai") -> "Brother"
            q.contains("sister") || q.contains("didi") || q.contains("behen") -> "Sister"
            else -> null
        }

    private fun normalizeQuery(raw: String): String =
        raw.lowercase()
            .replace(Regex("\\b(call|phone|dial|lagao|karo|ko|whatsapp|message|bhejo|to|golu)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
