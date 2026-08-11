# Contributing to Momentum

Thanks for helping build Momentum. This is a small, opinionated, offline-first app — please
read this before opening a PR.

## No AI-generated code

Contributions must be written by a human. Do not submit code generated (in whole or in part)
by ChatGPT, Claude, Copilot, Cursor, or any other AI coding tool, and do not submit code you
can't personally explain line-by-line in review.

- This applies to production code, tests, and Gradle/config files alike.
- Using an AI tool to search docs, explain an error message, or brainstorm is fine — but the
  code you commit has to be yours, understood and written by you.
- PRs that look AI-generated (unnatural comment style, boilerplate the diff doesn't need,
  inconsistent with the rest of the codebase, or a contributor who can't answer basic questions
  about their own change) will be closed without merging.
- If you're unsure whether something crosses the line, ask in the PR description before you
  submit, not after.

## Getting set up

​```
git clone <repo>
cd momentum
./gradlew assembleDebug
./gradlew test
​```

Requires a normal Android SDK setup (Android Studio handles this for you) — `compileSdk`/
`targetSdk` 37, `minSdk` 26.

## Workflow

1. **Branch** off `main`: `git checkout -b your-name/short-description`
2. **Make your change.** Keep PRs focused — one feature or fix per PR.
3. **Test it.** Run `./gradlew test` for unit tests, and actually run the app for anything
   UI-facing (Today/Detail/Insights/Add-Edit screens, the widget). Add unit tests for any new
   logic in `domain/` (streak math, insights, grid calculations) — that package is pure Kotlin
   and cheap to test, there's no excuse to skip it.
4. **Push and open a PR** against `main`. Describe *what* changed and *why*, not just what the
   diff shows.
5. Expect review comments. Please respond to them instead of re-pushing silently.

If you don't have push access, fork the repo, push to your fork, and open the PR from there.

## Code style

- Kotlin, standard `.editorconfig`/`ktlint`-style formatting (4-space indent, trailing commas
  in multi-line calls, no wildcard imports).
- Follow the existing MVVM layering: `domain/` stays pure Kotlin (no Android imports), `data/`
  owns Room/DataStore/repositories, `ui/` is Compose, `widget/` is Glance.
- **Color**: `ui/theme/Theme.kt` is the single source of truth for every color in the app,
  including the widget. Don't hardcode a hex value anywhere else — add it to `Theme.kt` and
  reference it from there.
- No dynamic/Material You color, no drop shadows or elevation, hairline dividers instead of
  card borders — this is a deliberate, minimal visual language, not an oversight.
- Comments should explain *why*, not *what*. If a comment just restates the code, delete it.

## Commit messages

Short, imperative, explain the reasoning if it's not obvious from the diff:

​```
Fix weekly-target streak counting an in-progress week as broken
​```

Not:

​```
update code
fix bug
​```

## Reporting bugs / requesting features

Open an issue with repro steps (for bugs) or the use case (for features). "It doesn't work"
without steps to reproduce will just get a request for more info.
