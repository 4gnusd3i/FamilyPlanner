# UI Regression Tests

This project contains the browser-based functional regression baseline for `FamilyPlanner`.

What it covers:
- first-run setup redirect/lockout behavior
- kiosk layout above width threshold and stacked layout below threshold regression
- responsive overflow protection
- viewport-fit and non-overlap checks for major planner regions, including desktop samples at 1366x768, 1920x1080, and 2560x1440
- touch-target sizing checks for primary interactive controls
- event create, edit, delete, and recurrence series behavior
- meal create, edit, and delete
- budget update, expense add, and expense delete
- shopping create, toggle, edit, and delayed auto-delete
- note create, view, edit, and delete
- family member create, profile edit, drag/drop event prefill, and delete
- API bad-request validation for malformed JSON mutation commands and invalid recurrence values
- storage maintenance for obsolete collections, expired lifecycle items, removed feature collection purge, setup reset cleanup, and avatar extension validation

First-time setup:
```powershell
.\Install-UiRegressionBrowser.ps1
```

Run the suite:
```powershell
.\Run-UiRegression.ps1
```

Artifacts from each run are written to:
- `tests/FamilyPlanner.UiTests/.artifacts/`

Notes:
- the suite starts its own local app instance on a free localhost port
- seeded test data is created through the app's own endpoints before each test
- Playwright browsers are stored under `.playwright-browsers/`
- avatar-to-calendar drag/drop is exercised through HTML5 drag event dispatch so the same browser event path is covered deterministically in automation
