# Scheduled maintenance repair — 18 October 2026

Expected: scheduled SAS/Localazy synchronization completes without upstream-only branch, token or label dependencies; generated changes remain unmerged PRs. Nightly dependency scanning stays enabled on the shipped managed runtime graph and retains CVSS 7 and unused-suppression failures.

Evidence: SAS run 39196813635 failed fetching absent develop after successful import. Nightly run 39114886037 failed on one unused suppression (SHA1 10b40679206af54442b508c11c3253fa25e5bd9e). Localazy run 38208578625 failed resource merge because imported common_media_upload_quality/common_moderation_and_safety duplicated project-owned temporary.xml resources.

Changes: scope SAS/Localazy to this repository, give job-only contents/PR write permissions, use default-branch checkout/PR base, existing enhancement label and built-in token; serialize each sync. Restrict SAS staged paths to generated SAS XML. Localazy removes imported names already owned by same-locale temporary.xml without modifying that file, before resource generation/build checks; fail immediately on script errors. Remove only the single proven unused suppression, retaining all scan gates and remaining exact artifact exclusions. No schedules/checks/notifications are disabled. Existing release workflows, personal token grants and other repositories are unchanged.

Local audit: YAML and workflow contracts passed; whitespace diff passed. Temporary-resource fixture checks passed for duplicate strings/plurals, Unicode, xliff preservation, idempotency, unchanged temporary.xml and malformed XML rejection. These are local fixtures, not remote acceptance. Security gate settings remain unchanged. No app source, release version, native/web/session/payment logic or installed app is changed; device/UI/iOS acceptance is not applicable to repository maintenance.

Remote acceptance pending publication and workflow_dispatch runs of SAS, Localazy and Nightly reports. Record actual outcomes below before completion. Generated localization/Gradle PRs are separate review work and must not be merged automatically.
