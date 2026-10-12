// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.messages.impl.timeline.factories.event

import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MessageShield
import io.element.android.libraries.matrix.api.timeline.item.event.MessageShieldProvider
import io.element.android.libraries.matrix.api.user.AntintwrkMatrixIdentity

/** Presentation under the approved operator-custodial account recovery model, not a crypto trust override. */
internal fun EventTimelineItem.messageShieldForAccountUi(
    actor: UserId,
    managed: Boolean = io.element.android.libraries.matrix.api.BuildConfig.ANTINTWRK_NATIVE_AUTH,
): MessageShieldProvider {
    if (!managed) return messageShieldProvider
    val shield = messageShieldProvider(false)
    val visible = if (forwarder == null && AntintwrkMatrixIdentity.sameRealm(actor.value, sender.value) &&
        shield is MessageShield.AuthenticityNotGuaranteed && !shield.isCritical
    ) {
        // Backup-based keys cannot prove a direct device-to-device delivery path. That
        // informational notice is not part of our account-managed UX. Preserve the raw
        // provider, strict inspection and every actual mismatch/identity/device warning.
        null
    } else {
        shield
    }
    return AccountShieldPresentation(messageShieldProvider, visible)
}

// Stable equality avoids emitting a new timeline solely because another profile/receipt refresh ran.
// A changed crypto result still changes this snapshot, even when the raw provider itself is reused.
private data class AccountShieldPresentation(
    val raw: MessageShieldProvider,
    val visible: MessageShield?,
) : MessageShieldProvider {
    override fun invoke(strict: Boolean): MessageShield? = if (strict) raw(true) else visible
}
