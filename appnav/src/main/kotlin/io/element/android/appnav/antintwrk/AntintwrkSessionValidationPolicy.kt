// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

/** Retaining a previously admitted surface on transport failure never grants a new session. */
internal fun antintwrkRetainAdmission(revalidate: Boolean, admitted: Boolean, errorCode: String? = null): Boolean =
    revalidate && admitted && errorCode in setOf(null, "", "UNAVAILABLE", "RATE_LIMITED")
