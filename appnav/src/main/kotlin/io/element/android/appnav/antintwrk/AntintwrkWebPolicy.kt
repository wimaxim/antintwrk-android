/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.element.android.appnav.antintwrk

import android.net.Uri

/** Routes are presentation hints; account and action permissions stay on the server. */
internal class AntintwrkWebPolicy(val origin: String) {
    data class MessengerLink(val target: String?)
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
        "privacypolicy",
        "dmca",
        "community",
        "safety",
        "verification",
    )

    /** Canonicalise legacy navigation only; legacy origins never become trusted JS origins. */
    fun canonical(url: String): String {
        if (publicAssistance(url)) return url
        val uri = Uri.parse(url)
        val target = if (uri.scheme == "https" && uri.host in setOf("antimsg.com", "antintwrk.com", "www.antintwrk.com") &&
            uri.userInfo == null && uri.port in setOf(-1, 443)) {
                uri.buildUpon().authority(base.authority).build()
            } else {
                uri
            }
        val parts = target.pathSegments
        if (sameOrigin(target.toString()) && parts.size == 2 && parts[0] in languages) {
            val alias = when (parts[1]) {
                "useragreement" -> "terms"
                "privacypolicy" -> "privacy"
                else -> null
            }
            if (alias != null) return target.buildUpon().path("/${parts[0]}/$alias").build().toString()
        }
        return target.toString()
    }
    fun sameOrigin(url: String): Boolean {
        val uri = Uri.parse(url)
        return uri.scheme == "https" && uri.host == base.host && uri.port == base.port && uri.userInfo == null
    }
    fun messengerLink(url: String): MessengerLink? {
        if (!sameOrigin(url)) return null
        val uri = Uri.parse(url)
        val parts = uri.pathSegments
        if (parts.size !in 2..3 || parts[0] !in languages || parts[1] != "messages") return null
        if (parts.size == 3) {
            if (uri.getQueryParameters("with").isNotEmpty()) return null
            return parts[2].takeIf { it.matches(Regex("[A-Za-z0-9_-]{16,128}")) }?.let { MessengerLink("room:$it") }
        }
        val targets = uri.getQueryParameters("with")
        if (targets.isEmpty()) return MessengerLink(null)
        return targets.singleOrNull()?.takeIf { it.matches(Regex("[1-9][0-9]{0,17}")) }?.let(::MessengerLink)
    }
    fun route(language: String, path: String): String =
        "$origin/${language.takeIf { it in languages } ?: "en"}/$path?app=1"

    /** A read-only registration reader accepts only our exact localized legal routes. */
    fun legalReader(input: String): String? {
        val url = canonical(input)
        if (!sameOrigin(url)) return null
        val uri = Uri.parse(url)
        val parts = uri.pathSegments
        if (parts.size != 2 || parts[0] !in languages || parts[1] !in setOf("terms", "useragreement", "privacy", "privacypolicy")) return null
        return uri.buildUpon().clearQuery().appendQueryParameter("native_legal", "1").build().toString()
    }
    fun feed(language: String): String = route(language, "hero-feed")

    /** Same authenticated transition as the web menu; destination stays server-controlled. */
    fun game(language: String): String {
        val selected = language.takeIf { it in languages } ?: "en"
        return Uri.parse(route(selected, "goto")).buildUpon().appendQueryParameter("to", "/$selected/").build().toString()
    }
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
        if (uri.queryParameterNames.contains("app_ref")) return null
        if (uri.path == "/_firstvisit.html") {
            val destination = uri.getQueryParameter("url") ?: return null
            if (Uri.parse(destination).path == "/_firstvisit.html" || embedded(destination) == null) return null
            return url
        }
        val parts = uri.pathSegments
        if (parts.size < 2 || parts[0] !in languages || parts[1] !in routes) return null
        if (parts[1] == "messages" && messengerLink(url) == null) return null
        return uri.buildUpon().clearQuery().apply {
            uri.queryParameterNames.filter { it != "app" }.forEach { name ->
                uri.getQueryParameters(name).forEach { value -> appendQueryParameter(name, value) }
            }
            appendQueryParameter("app", "1")
        }.build().toString()
    }
    fun section(url: String): AntintwrkSection? {
        if (embedded(url) == null) return null
        val parts = Uri.parse(url).pathSegments
        if (parts.size != 2) return null
        return when (parts[1]) {
            "communities" -> AntintwrkSection.Communities
            "profile" -> AntintwrkSection.Profile
            "hero-feed" -> AntintwrkSection.Feed
            else -> null // All other pages use the separate additional screen.
        }
    }
    fun publicAssistance(url: String): Boolean = Uri.parse(url).let { uri ->
        uri.scheme == "https" && uri.host == "antintwrk.com" && uri.port == -1 && uri.userInfo == null && uri.fragment == null &&
            uri.pathSegments.size == 2 && uri.pathSegments[0] in languages && uri.pathSegments[1] == "ukraine" &&
            uri.queryParameterNames == setOf("app_ref") &&
            uri.getQueryParameters("app_ref").singleOrNull()?.matches(Regex("[A-Za-z0-9_-]{1,1456}\\.[A-Za-z0-9_-]{43}")) == true
    }
    fun external(url: String): Boolean = publicAssistance(url) || Uri.parse(url).let { uri ->
        uri.scheme in setOf("https", "http", "mailto", "tel") && uri.userInfo == null &&
            uri.host !in setOf(base.host, "antimsg.com", "antintwrk.com", "www.antintwrk.com")
    }
}
