# Contributing

## Commit Messages

Write commit messages for humans first. The first line should explain the user-visible or maintainer-relevant change clearly enough to appear in release notes without rewriting.

Use this format:

```text
type(scope): summary
```

Examples:

```text
feat(ui): add commissioning progress details
fix(matter): fail closed when CHIP controller readiness fails
ci(release): publish APK with generated changelog
```

Preferred types are `feat`, `fix`, `matter`, `chip`, `ci`, `build`, `release`, `docs`, `test`, and `chore`. Useful scopes include `matter`, `chip`, `commissioning`, `ocw`, `thread`, `ble`, `ui`, and `release`.

Use `!` after the type or scope for incompatible behavior changes, and add `BREAKING CHANGE:` in the commit body when the subject is not enough.

If a commit should not appear in generated release notes, include `[skip changelog]` or `changelog: skip`.

Avoid vague subjects like `fix stuff`, `update`, or `cleanup`. Prefer short, specific subjects written from the app user's or maintainer's point of view.

## Release Notes

Release notes are generated from commit history:

```powershell
node scripts/generate_changelog.mjs --tag v0.1.0
```

The release workflow uses the same script when publishing a GitHub release.
