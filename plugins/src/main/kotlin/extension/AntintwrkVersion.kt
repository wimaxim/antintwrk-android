/*
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package extension

import org.gradle.api.Project

/** One value for the installed app and its social HTTP/WebView identity. */
fun Project.antintwrkVersionName(): String = providers.gradleProperty("antintwrkVersionName").orElse("1.0.21").get()
    .replace(Regex("(?i)(?:^|[-.])test(?=[-.]|$)"), "").trim('-', '.')
    .also { require(it.matches(Regex("[0-9][A-Za-z0-9.+-]{0,63}"))) { "Own version name must be a bounded HTTP-safe token." } }
