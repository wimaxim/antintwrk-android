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
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

internal class AntintwrkNativeCredentials(context: Context, private val manager: CredentialManager = CredentialManager.create(context)) {
    companion object {
        private val operations = AntintwrkCredentialOperations()
        fun cleanupToken(): Long = operations.cleanupToken()
    }

    suspend fun signIn(context: Context, client: AntintwrkNativeAuthClient, provider: String): AntintwrkAuthSession = operations.authenticate {
        val challenge = client.providerChallenge(provider)
        val options = challenge.options
        val option = if (provider == "passkey") {
            GetPublicKeyCredentialOption(options.getJSONObject("options").toString())
        } else {
            GetSignInWithGoogleOption.Builder(options.getString("clientId")).setNonce(options.getString("nonce")).build()
        }
        val result = try {
            manager.getCredential(MutableContextWrapper(context), GetCredentialRequest(listOf(option))).credential
        } catch (_: GetCredentialCancellationException) {
            throw AntintwrkCredentialDismissed()
        }
        val credential = when {
            provider == "passkey" && result is PublicKeyCredential -> result.authenticationResponseJson
            provider == "google" && result is CustomCredential && result.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
                GoogleIdTokenCredential.createFrom(result.data).idToken
            else -> error("Unexpected credential type")
        }
        client.providerSignIn(provider, credential, options.getString("ticket"), challenge.csrf)
    }
    suspend fun clear(token: Long) {
        operations.clear(token) { manager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}

internal class AntintwrkCredentialDismissed : Exception()
