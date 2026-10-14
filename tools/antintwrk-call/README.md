# Embedded call pickup replay overlay

The outcome of a DM ring is shared by the auto-leave and ringing UI consumers.
Upstream `scope.share` loses an immediately completed outcome for the second
consumer. A fast accepted call can consequently keep its ringing tile and tone.
`call-pickup.patch` retains that one outcome in a scope-owned Behavior, filters
the initial null and emits exactly once to each consumer. Recipient and decline
relation checks, expiry, encryption, transport and audio playback remain intact.

This is a source-equivalent, checksum-pinned overlay, not a dependency-cache edit
or global audio mute. Android Gradle packages the complete pinned AAR assets in a generated
`element-call-antintwrk-pickup-v1` directory. The original asset-only AAR
(empty classes.jar) is no longer a runtime dependency. The new local URL avoids
reusing previously cached call chunks. iOS copies the original Swift-package dist into its own app resources and
applies the same change before signing; it retains embedded version 0.25.0.
An unrecognised package fails the build rather than silently shipping unpatched.

## Corresponding source and reproduction

- Android source: Element Call v0.26.0, commit
  `71593f1a245b1afa971e3940596e1c500edf9548`.
- iOS source: Element Call v0.25.0, commit
  `efde51b8c1e136fca17d38ec408b0a7dbb2f17e9`.
- Upstream: https://github.com/element-hq/element-call
- Apply `call-pickup.patch` with `git apply` in either pinned checkout.
- `call-pickup-tests.patch` adds the two reproducible synchronous consumer-order
  regressions to the upstream lifecycle suite (validated with v0.26.0).
- Preserve upstream AGPL/commercial notices. These own modifications are AGPL-3.0-only.

The packaged overlay does not require Node or a fresh web build. It verifies the
input JS SHA-256 and a single exact expression before changing it. Its marker
records input/output hashes. The modified main chunk omits its stale automatic
source-map link; use the pinned source and patch above for that function.
Other source maps remain unchanged, and upstream asset/import names are retained.

For fixture tests only, install the pinned web checkout's frozen dependencies.
`test_packaged_pickup.mjs` executes the actual original/patched minified function
with RxJS and the original `ObservableScope` compiled to an ESM file. Pass that
file, original JS, patched JS and embedded version as its four arguments. It
first reproduces the original lost pickup, then checks accept/decline/timeout in
both consumer orders, including late observation. This is not a real media call.

14 October checks: original lifecycle 10 pass/2 regressions fail; patched
lifecycle plus CallViewModel suites 74 pass/4 existing skipped. Both actual
packaged versions pass all six consumer-order/outcome cases. Android APK and
signed iOS Simulator app contain the verified output. Physical cross-device
audio acceptance and distribution remain separate.
