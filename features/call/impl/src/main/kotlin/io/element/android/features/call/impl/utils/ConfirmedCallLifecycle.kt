// Copyright 2026 Super David Universe LLC
// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.features.call.impl.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile

/** Initial absent cached membership is unknown, not an observed hangup. State is per call. */
internal fun Flow<Pair<Boolean, Boolean>>.confirmedCallLifecycle(): Flow<Pair<Boolean, Boolean>> =
    dropWhile { (active, answeredElsewhere) -> !active && !answeredElsewhere }.distinctUntilChanged()
