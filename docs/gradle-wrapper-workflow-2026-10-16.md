# Gradle Wrapper workflow repair — 16 October 2026

The scheduled run [39000096794](https://github.com/wimaxim/antintwrk-android/actions/runs/39000096794) on cc7821c069 failed before wrapper processing: `repo-token cannot be empty`. The upstream-only DANGER_GITHUB_API_TOKEN secret is absent. The workflow also targeted develop and PR-Build, rather than this fork's default branch and an existing label.

Use the built-in short-lived github.token, with contents/write and pull-requests/write confined to this job. Checkout, base and PR target resolve to the repository default branch (verified as codex/antintwrk-android). Use the existing enhancement label. Limit execution to wimaxim/antintwrk-android, retain manual/scheduled triggers, checksum generation and pinned actions. No personal token is stored, no automatic merge is enabled, and Gradle/app sources are unchanged.

## Audit

- Verified: actual failed remote log identifies the missing token; the Node20 notice is not the thrown error.
- Verified: GitHub API reports default branch codex/antintwrk-android and existing enhancement label; PR-Build is absent. Pinned action.yml supports github.token, base-branch and target-branch.
- Verified: YAML parsing and git diff whitespace checks passed before commit. Only the workflow and this report belong to this change.
- Usability/efficiency: scheduled maintenance requires no recurring personal-token management; updates remain reviewable PRs. Existing Actions-token PRs do not automatically trigger ordinary push/pull_request workflows, so a maintainer must run the required checks before merging.
- Blocked remote acceptance: repository workflow settings report default_workflow_permissions=read and can_approve_pull_request_reviews=false. Keep default read permissions; the repository's Allow GitHub Actions to create and approve pull requests option must be enabled for built-in-token PR creation. No auto-approval step is added by this workflow.
- Not verified: successful remote run, PR creation, retry and no-update completion, because this local commit has not been pushed and repository settings have not been changed. Rerunning the old run cannot test the fix. After authorized publication, run workflow_dispatch on the default branch and inspect its conclusion and any generated PR/checks.
- Not applicable: Android/iOS/web UI, device, payment, session and store checks; this change only repairs repository maintenance and does not modify application/runtime/build versions. No app release is produced.

Acceptance remains incomplete until the repaired workflow runs successfully on GitHub. GitHub push and repository setting changes require the owner's separate instruction.
