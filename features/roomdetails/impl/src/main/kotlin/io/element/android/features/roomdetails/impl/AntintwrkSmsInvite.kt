// Copyright 2026 Maxim Novichkov
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.roomdetails.impl

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.element.android.libraries.androidutils.R as AndroidUtilsR
import io.element.android.libraries.ui.strings.R as CommonStrings

/** A single contact URI grant; no address-book upload, broad contacts permission or SEND_SMS permission. */
@Composable
internal fun rememberAntintwrkSmsInvite(): () -> Unit {
    val context = LocalContext.current
    val sms = context.getString(CommonStrings.string.screen_ant_invite_sms)
    fun unavailable() = Toast.makeText(context, AndroidUtilsR.string.error_no_compatible_app_found, Toast.LENGTH_LONG).show()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runCatching {
                val uri = requireNotNull(result.data?.data)
                require(uri.scheme == "content" && uri.authority == ContactsContract.AUTHORITY)
                val phone = context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
                context.startActivity(antintwrkSmsIntent(requireNotNull(phone), sms))
            }.onFailure { unavailable() }
        }
    }
    return {
        runCatching { picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) }.onFailure { unavailable() }
    }
}

internal fun antintwrkSmsIntent(phone: String, message: String): Intent {
    val number = phone.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
    require(number.isNotEmpty() && number.length <= 40)
    return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).putExtra("sms_body", message)
}
