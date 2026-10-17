# Gradle Wrapper workflow repair — 16 October 2026

The scheduled run [39000096794](https://github.com/wimaxim/antintwrk-android/actions/runs/39000096794) on cc7821c069 failed before wrapper processing: `repo-token cannot be empty`. The upstream-only DANGER_GITHUB_API_TOKEN secret is absent. The workflow also targeted develop and PR-Build, rather than this fork's default branch and an existing label.

Use the built-in short-lived github.token, with contents/write and pull-requests/write confined to this job. Checkout, base and PR target resolve to the repository default branch (verified as codex/antintwrk-android). Use the existing enhancement label. Limit execution to wimaxim/antintwrk-android, retain manual/scheduled triggers, checksum generation and pinned actions. No personal token is stored, no automatic merge is enabled, and Gradle/app sources are unchanged.

## Audit

- Verified: actual failed remote log identifies the missing token; the Node20 notice is not the thrown error.
- Verified: GitHub API reports default branch codex/antintwrk-android and existing enhancement label; PR-Build is absent. Pinned action.yml supports github.token, base-branch and target-branch.
- Verified: YAML parsing and git diff whitespace checks passed before commit. Only the workflow and this report belong to this change.
- Usability/efficiency: scheduled maintenance requires no recurring personal-token management; updates remain reviewable PRs. Existing Actions-token PRs do not automatically trigger ordinary push/pull_request workflows, so a maintainer must run the required checks before merging.
- Initial remote blocker (resolved after owner approval): repository workflow settings report default_workflow_permissions=read and can_approve_pull_request_reviews=false. Keep default read permissions; the repository's Allow GitHub Actions to create and approve pull requests option was subsequently enabled for built-in-token PR creation, with default permissions retained as read. No auto-approval step is added by this workflow.
- Verified remote acceptance: owner approved publication and the repository setting. Remote default branch contains 3123b2763ffc81ea2e1c558fe29475b05ee1fddf. Manual run [39000805629](https://github.com/wimaxim/antintwrk-android/actions/runs/39000805629) completed successfully in 7m56s on 17 October UTC (16 October PDT). Both Gradle wrapper generation and verification reported BUILD SUCCESSFUL. The workflow created [PR #1](https://github.com/wimaxim/antintwrk-android/pull/1), upgrading 9.7.1 to 9.8.1 against codex/antintwrk-android with enhancement and gradle-wrapper labels. Only wrapper jar, properties and Windows launcher changed; autoMergeRequest is null. PR remains open, not merged.
- Not verified: retry/duplicate and no-update branches, or application compatibility with proposed Gradle 9.8.1. The maintenance workflow itself passed; the generated dependency update still requires its own review and checks before merging.
- Not applicable: Android/iOS/web UI, device, payment, session and store checks; this change only repairs repository maintenance and does not modify application/runtime/build versions. No app release is produced.

The repaired maintenance workflow has passed real GitHub acceptance. This does not claim Android application acceptance on the proposed Gradle update. No app build/release or PR merge was requested or performed.
