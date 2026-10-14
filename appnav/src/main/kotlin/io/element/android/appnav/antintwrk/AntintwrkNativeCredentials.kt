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
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.DataError
import androidx.credentials.exceptions.domerrors.NetworkError
import androidx.credentials.exceptions.domerrors.NotAllowedError
import androidx.credentials.exceptions.domerrors.SecurityError
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
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
        } catch (error: GetCredentialException) {
            if (provider != "passkey") throw error
            // Fixed categories only: provider messages can contain identifiers or signed data.
            throw AntintwrkPasskeyFailure(
                when (error) {
                is NoCredentialException -> "PK-NO-KEY"
                is GetCredentialProviderConfigurationException, is GetCredentialUnsupportedException -> "PK-CONFIG"
                is GetCredentialInterruptedException -> "PK-INTERRUPTED"
                is GetPublicKeyCredentialDomException -> when (error.domError) {
                    is SecurityError -> "PK-SECURITY"
                    is DataError -> "PK-DATA"
                    is NotAllowedError -> "PK-DENIED"
                    is NetworkError -> "PK-NETWORK"
                    else -> "PK-PROVIDER"
                }
                else -> "PK-SYSTEM"
            }
            )
        }
        val credential = when {
            provider == "passkey" && result is PublicKeyCredential -> result.authenticationResponseJson
            provider == "google" && result is CustomCredential && result.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
                GoogleIdTokenCredential.createFrom(result.data).idToken
            provider == "passkey" -> throw AntintwrkPasskeyFailure("PK-RESPONSE")
            else -> error("Unexpected credential type")
        }
        client.providerSignIn(provider, credential, options.getString("ticket"), challenge.csrf)
    }
    suspend fun clear(token: Long) {
        operations.clear(token) { manager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}

internal class AntintwrkCredentialDismissed : Exception()

internal class AntintwrkPasskeyFailure(val code: String, val verification: Boolean = false) : Exception()
