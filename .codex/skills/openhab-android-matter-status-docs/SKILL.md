---
name: openhab-android-matter-status-docs
description: Use when updating openhab-android-matter project status, workflow, or research docs after implementation changes, hardware validation, or debugging findings.
---

# Status Docs

Use this skill when implementation reality changes, real-device validation succeeds/fails, workflow parameters change, or debugging reveals durable project knowledge.

## Primary Files

- `docs/implementation-status.md`: current capability status.
- `docs/open-commissioning-window-workflow.md`: OCW flow, parameters, source map, and failure paths.
- `docs/research.md`: architectural background.
- `README.md`: build/install basics; check for stale claims after major changes.

When docs conflict, status/workflow docs should be updated first and treated as more current than `README.md`.

## Update Rules

- Move items between implemented and not implemented when hardware validation changes reality.
- Update workflow docs when classes, parameters, failure paths, or sequencing change.
- Keep entries factual and concise.
- Do not write session narrative into permanent status docs.
- Do not permanently document REST tokens, Thread datasets, setup PINs, fabric material, or full setup codes.
- Mention device models/serials only when they are useful validation context and not sensitive.

## Verification

Before committing docs:

```powershell
rg -n "TBD|TODO|FIXME" docs
rg -n "password|token|dataset active" docs
git diff -- docs\implementation-status.md docs\open-commissioning-window-workflow.md docs\research.md README.md
```

Stage only intended docs. Leave unrelated app code, `.artifacts/`, and local backup files unstaged unless explicitly requested.

