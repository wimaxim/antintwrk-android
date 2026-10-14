# Google Play authentication repair — 14 October 2026

The owner installed the internal Play build1.0.37 and reported failed Google sign-in and a messenger that never finished preparing.

The actual MAS log rejected native client registration with invalid_redirect_uri. The release advertised Element client_uri=https://element.io/apps/android but used the own redirect com.antintwrk.app:/. The same posted metadata reproduced HTTP400 on the live MAS; matched ANTINTWRK metadata returned201. No server policy exception is needed or applied.

OAuthConfigurationProvider now uses the managed ANTINTWRK client domain, logo and guest Terms/Privacy URLs. Unmanaged upstream metadata stays intact. Debug and nightly own schemes derive from com.antintwrk.app; release keeps com.antintwrk.app:/. Existing OAuth sessions, identities, history and custody are preserved; no forced logout or reset.

Three focused OAuthConfigurationProvider tests and matrix impl ktlint passed with antintwrkNativeAuth=true. Tests check the native redirect namespace against the advertised domain, same-origin HTTPS brand/legal URLs and upstream configuration isolation. Live MAS accepted release/debug pairs and rejected a foreign callback. A real ordinary QA social account completed the release registration, native handoff, MAS consent/PKCE token exchange and matching Matrix whoami200. This is API integration validation, not physical Play-installed UI acceptance.

The Google Android OAuth client ANTINTWRK Android Google Play was created in the existing effective-fire-159508 project for com.antintwrk.app and the actual Play distribution SHA1 4D:39:5A:54:6C:67:17:FF:FD:72:17:C7:72:7F:F2:AD:58:BD:E6:9B. Existing debug/iOS/web clients and requested scopes are unchanged. The web client ID remains the existing server-supplied audience; no rebuild is needed just for that Google association. Google warns propagation can take five minutes to a few hours.

Release1.0.38/code380 was built with the normal managed release flags, own release Firebase resources and existing upload key. Release/R8/lintVital/app ktlint, bundletool validation and jarsigner verification passed. AAB SHA25641f0818d6073825c40815a944267d66696e1c26b84bc329d4053a69d87426eb4;165248860bytes; packagecom.antintwrk.app; debuggable=false. Self-signed upload-key chain warnings are distinct from the Play distribution signature. This task targets the existing internal test track; no production publication or new legal agreements.

Physical owner confirmation of Google sign-in and messenger opening on the new Play update is still required. iOS must keep its own matched HTTPS redirect/client domain and Google iOS association; Android-specific package, scheme and SHA1 do not apply to it. Never fix a registration mismatch by allowing arbitrary redirects or resetting accounts/keys.
