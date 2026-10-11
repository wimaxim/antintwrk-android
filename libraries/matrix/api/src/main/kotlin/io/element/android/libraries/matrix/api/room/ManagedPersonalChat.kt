// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.libraries.matrix.api.room

/** Stable direct metadata also covers an invitation or a DM whose peer has left. */
fun RoomInfo.isManagedPersonalChat(): Boolean = !isSpace && (isDm || isDirect) && activeMembersCount <= 2
