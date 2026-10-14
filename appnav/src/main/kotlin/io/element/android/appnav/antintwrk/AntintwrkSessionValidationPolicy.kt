// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

/** Retaining a previously admitted surface on transport failure never grants a new session. */
internal fun antintwrkRetainAdmission(revalidate: Boolean, admitted: Boolean, errorCode: String? = null): Boolean =
    revalidate && admitted && errorCode in setOf(null, "", "UNAVAILABLE", "RATE_LIMITED")

/** Returning from credential UI must not replace a login attempt's result/error. */
internal fun antintwrkRevalidateOnResume(stage: AntintwrkAuthStage?, busy: Boolean, page: AntintwrkPage): Boolean =
    stage == AntintwrkAuthStage.AUTHENTICATED ||
        stage == null && (!busy || page == AntintwrkPage.Welcome) ||
        stage in setOf(AntintwrkAuthStage.DEVICE, AntintwrkAuthStage.TOTP, AntintwrkAuthStage.SETUP_REQUIRED) && !busy
