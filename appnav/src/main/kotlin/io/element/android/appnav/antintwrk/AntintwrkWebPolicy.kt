/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.net.Uri

/** Routes are presentation hints; account and action permissions stay on the server. */
internal class AntintwrkWebPolicy(private val origin: String) {
    private val base = Uri.parse(origin)
    private val languages = setOf("en", "ru", "uk", "de", "es", "fr", "it", "ar", "zh")
    private val routes = setOf(
        "hero-feed",
        "hero",
        "channel",
        "group",
        "discover",
        "reels",
        "stories",
        "hero-compose",
        "communities",
        "create-community",
        "communities-people",
        "profile",
        "deleteaccount",
        "notifications",
        "friend-requests",
        "people",
        "messages",
        "monetization",
        "my-referrals",
        "kindness",
        "support-project",
        "ukraine",
        "how-it-works",
        "sms-consent",
        "about",
        "contacts",
        "support",
        "terms",
        "useragreement",
        "privacy",
        "dmca",
        "community",
        "safety",
    )

    /** Canonicalise legacy navigation only; legacy origins never become trusted JS origins. */
    fun canonical(url: String): String {
        val uri = Uri.parse(url)
        return if (base.host == "antimsg.com" && uri.scheme == "https" && uri.host in setOf("antintwrk.com", "www.antintwrk.com") &&
            uri.userInfo == null && uri.port in setOf(-1, 443)) {
                uri.buildUpon().authority("antimsg.com").build().toString()
            } else {
                url
            }
    }
    fun sameOrigin(url: String): Boolean {
        val uri = Uri.parse(url)
        return uri.scheme == "https" && uri.host == base.host && uri.port == base.port && uri.userInfo == null
    }
    fun route(language: String, path: String): String =
        "$origin/${language.takeIf { it in languages } ?: "en"}/$path?app=1"
    fun feed(language: String): String = route(language, "hero-feed")
    fun root(language: String, section: AntintwrkSection, ownProfileIdentity: String? = null): String = route(
        language,
        when (section) {
        AntintwrkSection.Feed -> "hero-feed"
        AntintwrkSection.Communities -> "communities"
        AntintwrkSection.Profile -> ownProfileIdentity?.let { "hero/$it" } ?: "profile"
    }
    )
    fun outbound(url: String): Boolean {
        val parts = Uri.parse(url).pathSegments
        val route = if (parts.firstOrNull() in languages) parts.getOrNull(1) else parts.firstOrNull()
        return route in setOf("go", "goto", "go.html")
    }
    fun needsAuth(url: String): Boolean = sameOrigin(url) && (
        Uri.parse(url).pathSegments.any { it in setOf("activitycheck", "g2fa", "logoff") } ||
            Uri.parse(url).getQueryParameter("auth") != null
        )
    fun embedded(input: String): String? {
        val url = canonical(input)
        if (!sameOrigin(url) || outbound(url) || needsAuth(url)) return null
        val uri = Uri.parse(url)
        if (uri.path == "/_firstvisit.html") {
            val destination = uri.getQueryParameter("url") ?: return null
            if (Uri.parse(destination).path == "/_firstvisit.html" || embedded(destination) == null) return null
            return url
        }
        val parts = uri.pathSegments
        if (parts.size < 2 || parts[0] !in languages || parts[1] !in routes) return null
        return uri.buildUpon().clearQuery().apply {
            uri.queryParameterNames.filter { it != "app" }.forEach { name ->
                uri.getQueryParameters(name).forEach { value -> appendQueryParameter(name, value) }
            }
            appendQueryParameter("app", "1")
        }.build().toString()
    }
    fun section(url: String): AntintwrkSection? {
        if (embedded(url) == null) return null
        return when (Uri.parse(url).pathSegments.getOrNull(1)) {
            "communities", "create-community", "communities-people", "channel", "group" -> AntintwrkSection.Communities
            "profile", "deleteaccount", "monetization", "my-referrals", "kindness" -> AntintwrkSection.Profile
            "hero-feed", "reels", "stories", "hero-compose" -> AntintwrkSection.Feed
            else -> null // Search, notifications and other people's profiles retain their entry section.
        }
    }
    fun external(url: String): Boolean = Uri.parse(url).let { uri ->
        uri.scheme in setOf("https", "http", "mailto", "tel") && uri.userInfo == null &&
            uri.host !in setOf(base.host, "antintwrk.com", "www.antintwrk.com")
    }
}
