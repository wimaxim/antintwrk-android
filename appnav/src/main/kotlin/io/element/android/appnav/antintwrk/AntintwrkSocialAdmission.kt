// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity
import java.io.IOException
import javax.net.ssl.SSLException

/** An existing public session may survive RU ingress failure; never admit another account or bypass a challenge. */
internal suspend fun antintwrkRestoreSocialAdmission(
    expectedActor: String?,
    primary: suspend () -> AntintwrkAuthSession,
    fallback: suspend () -> AntintwrkAuthSession,
    acceptedFallback: () -> Unit,
): AntintwrkAuthSession {
    try {
        return primary()
    } catch (error: Exception) {
        val unavailable = error is IOException && error !is SSLException ||
            error is AntintwrkRequestError && error.httpStatus in setOf(408, 500, 502, 503, 504)
        if (!unavailable || AntintwrkMatrixIdentity.socialId(expectedActor.orEmpty()) == null) throw error
    }
    val candidate = fallback()
    if (candidate.stage != AntintwrkAuthStage.AUTHENTICATED || candidate.matrixUserId != expectedActor) {
        throw AntintwrkRequestError("AUTH_REQUIRED", 401)
    }
    acceptedFallback()
    return candidate
}
