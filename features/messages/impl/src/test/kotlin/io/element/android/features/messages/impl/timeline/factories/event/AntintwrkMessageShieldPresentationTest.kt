// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.messages.impl.timeline.factories.event

import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.fixtures.aTimelineItemContentFactory
import io.element.android.features.messages.impl.messagesummary.FakeMessageSummaryFormatter
import io.element.android.features.messages.impl.timeline.factories.TimelineItemsFactoryConfig
import io.element.android.libraries.dateformatter.test.FakeDateFormatter
import io.element.android.libraries.matrix.api.core.UniqueId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.EventTimelineItem
import io.element.android.libraries.matrix.api.timeline.item.event.MessageShield
import io.element.android.libraries.matrix.api.timeline.item.event.MessageShieldProvider
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.permalink.FakePermalinkParser
import io.element.android.libraries.matrix.test.timeline.aMessageContent
import io.element.android.libraries.matrix.test.timeline.anEventTimelineItem
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AntintwrkMessageShieldPresentationTest {
    private val actor = UserId("@u18:antintwrk.com")
    private val peer = UserId("@u19:antintwrk.com")
    private val notice = MessageShield.AuthenticityNotGuaranteed(false)

    private fun event(shield: MessageShield? = notice, sender: UserId = peer): EventTimelineItem = anEventTimelineItem(
        sender = sender,
        content = aMessageContent(),
        messageShieldProvider = MessageShieldProvider { shield },
    )

    @Test
    fun `account recovery notice is absent for incoming and outgoing managed messages`() {
        for (sender in listOf(actor, peer)) {
            val original = event(sender = sender)
            assertThat(original.messageShieldForAccountUi(actor, managed = true)(false)).isNull()
            // No SDK provenance or raw trust result is overwritten.
            assertThat(original.messageShieldProvider(false)).isEqualTo(notice)
        }
    }

    @Test
    fun `upstream presentation and strict inspection preserve the original result`() {
        val original = event()
        assertThat(original.messageShieldForAccountUi(actor, managed = false)(false)).isEqualTo(notice)
        assertThat(original.messageShieldForAccountUi(actor, managed = true)(true)).isEqualTo(notice)
    }

    @Test
    fun `foreign mismatched and malformed identities do not qualify for account recovery presentation`() {
        for ((source, sender) in listOf(
            UserId("@alice:example.org") to peer,
            actor to UserId("@alice:example.org"),
            actor to UserId("@u19:matrix.antimsg.com"),
            actor to UserId("@u0:antintwrk.com"),
            actor to UserId("@u19:antintwrk.com.evil.example"),
        )) {
            assertThat(event(sender = sender).messageShieldForAccountUi(source, managed = true)(false)).isEqualTo(notice)
        }
    }

    @Test
    fun `explicit third party history forwarding retains its warning`() {
        val forwarded = event().copy(forwarder = actor)
        assertThat(forwarded.messageShieldForAccountUi(actor, managed = true)(false)).isEqualTo(notice)
    }

    @Test
    fun `critical authenticity warnings are never hidden`() {
        val critical = MessageShield.AuthenticityNotGuaranteed(true)
        assertThat(event(critical).messageShieldForAccountUi(actor, managed = true)(false)).isEqualTo(critical)
    }

    @Test
    fun `actual identity device sender and encryption problems remain visible`() {
        for (critical in listOf(false, true)) {
            val problems = listOf(
                MessageShield.UnknownDevice(critical),
                MessageShield.UnsignedDevice(critical),
                MessageShield.UnverifiedIdentity(critical),
                MessageShield.VerificationViolation(critical),
                MessageShield.MismatchedSender(critical),
                MessageShield.SentInClear(critical),
            )
            for (problem in problems) {
                assertThat(event(problem).messageShieldForAccountUi(actor, managed = true)(false)).isEqualTo(problem)
            }
        }
    }

    @Test
    fun `an already verified message remains without a shield`() {
        assertThat(event(null).messageShieldForAccountUi(actor, managed = true)(false)).isNull()
    }

    @Test
    fun `unchanged metadata keeps stable equality but a changed crypto result refreshes presentation`() {
        var result: MessageShield? = notice
        val source = event().copy(messageShieldProvider = MessageShieldProvider { result })
        val first = source.messageShieldForAccountUi(actor, managed = true)
        assertThat(source.messageShieldForAccountUi(actor, managed = true)).isEqualTo(first)
        result = MessageShield.MismatchedSender(true)
        val next = source.messageShieldForAccountUi(actor, managed = true)
        assertThat(next).isNotEqualTo(first)
        assertThat(next(false)).isEqualTo(result)
    }

    @Test
    fun `UI factory removes notice for both consumers and refreshes real trust failures`() = runTest {
        val factory = TimelineItemEventFactory(
            config = TimelineItemsFactoryConfig(computeReadReceipts = false, computeReactions = false),
            contentFactory = aTimelineItemContentFactory(),
            matrixClient = FakeMatrixClient(sessionId = actor),
            dateFormatter = FakeDateFormatter(),
            permalinkParser = FakePermalinkParser(),
            summaryFormatter = FakeMessageSummaryFormatter(),
        )
        val original = MatrixTimelineItem.Event(UniqueId("test-event"), event())
        val ui = factory.create(original, 0, listOf(original), emptyList(), false)
        val managed = io.element.android.libraries.matrix.api.BuildConfig.ANTINTWRK_NATIVE_AUTH
        // Timeline badge/dialog and the long-press action sheet use this same property.
        if (managed) assertThat(ui.messageShield).isNull() else assertThat(ui.messageShield?.shield).isEqualTo(notice)
        assertThat(original.event.messageShieldProvider(false)).isEqualTo(notice)

        val problem = MessageShield.MismatchedSender(true)
        val changed = original.copy(event = event(problem))
        val updated = factory.update(ui, changed, emptyList(), false)
        assertThat(updated.messageShield?.shield).isEqualTo(problem)
        assertThat(updated.content).isEqualTo(ui.content)
        val verified = factory.update(updated, original.copy(event = event(null)), emptyList(), false)
        assertThat(verified.messageShield).isNull()
    }
}
