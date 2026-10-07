/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.content.Context
import android.content.MutableContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

internal class AntintwrkNativeCredentials(context: Context) {
    private val manager = CredentialManager.create(context)
    suspend fun signIn(context: Context, client: AntintwrkNativeAuthClient, provider: String, csrf: String): AntintwrkAuthSession {
        val options = client.providerOptions(provider, csrf)
        val option = if (provider == "passkey") {
            GetPublicKeyCredentialOption(options.getJSONObject("options").toString())
        } else {
            GetSignInWithGoogleOption.Builder(options.getString("clientId")).setNonce(options.getString("nonce")).build()
        }
        val result = manager.getCredential(MutableContextWrapper(context), GetCredentialRequest(listOf(option))).credential
        val credential = when {
            provider == "passkey" && result is PublicKeyCredential -> result.authenticationResponseJson
            provider == "google" && result is CustomCredential && result.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
                GoogleIdTokenCredential.createFrom(result.data).idToken
            else -> error("Unexpected credential type")
        }
        return client.providerSignIn(provider, credential, options.getString("ticket"), csrf)
    }
    suspend fun clear() {
        manager.clearCredentialState(ClearCredentialStateRequest())
    }
}
