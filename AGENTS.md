# AGENTS.md

Operating rules for this repository. Read this before changing files.

## Sources of truth

- The active GitHub issue, or its matching section in `issue_breakdown.md`, defines scope
  and acceptance criteria.
- Initial product assumptions live with their issue in `issue_breakdown.md`.
- `docs/product-decisions.md` records only decisions or deviations discovered after
  planning. A recorded deviation supersedes the corresponding initial assumption.
- This file defines cross-cutting engineering and workflow rules.

The original take-home brief may exist locally as the intentionally untracked
`requirement.md`. Treat it as optional background: its absence in a clone, CI job, or
worktree must never block implementation or review.

## Project and stack

Android shopping app for the 7shifts mobile take-home:

- Kotlin, Jetpack Compose, and Material 3.
- Single Gradle module, `:app`; boundaries are enforced by package.
- Hilt for dependency injection.
- Retrofit with kotlinx.serialization for networking.
- Coil for image loading.
- Coroutines and Flow for asynchronous work and state.
- JDK 17, compileSdk 36, targetSdk 36, minSdk 26.

Dependency versions belong in `gradle/libs.versions.toml`, never directly in a
`build.gradle.kts`. The versions are deliberately pinned for the current Android, Kotlin,
KSP, Hilt, Coil, and ktlint compatibility envelope. Do not add or upgrade a dependency
inside feature work without asking first; make a necessary upgrade its own issue.

## Architecture

```text
com.sevenshifts.shopping
  data/      DTOs, Retrofit services, repository implementations, mappers
  domain/    Models, repository interfaces, business rules
  ui/        Compose screens, state holders, theme, shared components
  di/        Hilt modules
```

The dependency rule is `ui -> domain <- data`:

- `domain` is plain Kotlin and imports nothing from Android, AndroidX, or `data`.
- `ui` depends on `domain` interfaces, never `data` implementations.
- `data` maps DTOs to domain models at its boundary; DTOs do not escape it.

## Engineering rules

- Money is `BigDecimal` from DTO mapping through domain and UI state. Format it as
  `$0.00` only at the Compose boundary; never use `Double` or `Float` for prices or totals.
- Hoist state. Screen composables take state and event lambdas. A screen-level `ViewModel`
  exposes one immutable UI state through `StateFlow`.
- Model absence explicitly; do not use `!!`.
- Each screen handles the states applicable to its data and actions—for example loading,
  empty, error/retry, submitting, success, or failure. Do not invent irrelevant states.
- Do not suppress compiler or lint findings or add a lint baseline. Version-availability
  warnings from the deliberately pinned dependency set are the known exception.
- Avoid unrelated refactors and speculative abstractions. Extract shared code when a
  second real use demonstrates the common shape.

## Verification

Before reporting an implementation complete, run:

```bash
./gradlew verify
```

It applies formatting, runs Android Lint and JVM tests, and assembles the debug APK. Fix
failures and rerun it; if blocked, report the command and relevant failure output.

CI runs `./gradlew verifyCi`, which performs the same checks but uses `spotlessCheck`
instead of rewriting files. Both aggregate tasks are defined in the root
`build.gradle.kts`. Do not invent substitute commands if either task is missing.

## Testing and evidence

- Derive tests and other evidence from the issue's behavior and risks, not from an
  arbitrary test count.
- Keep tests with the behavior they protect. Domain rules and state holders normally need
  focused JVM unit tests.
- Use Robolectric Compose tests when semantics or interaction are meaningful to assert on
  the JVM. Follow the established `ShoppingNavHostTest` harness.
- Robolectric proves behavior in the semantics tree, not visual correctness. A human checks
  UI changes on a phone-sized emulator.
- Name tests after behavior using readable backtick names.

Every acceptance criterion needs evidence, but not necessarily an automated test. Valid
evidence includes a named unit or Compose test, a Gradle/CI result, a document inspection,
or a recorded manual emulator check. In the final handoff, map each criterion to its
evidence and location or command.

## Recording decisions

Do not copy initial assumptions from `issue_breakdown.md` into
`docs/product-decisions.md`. Add an entry only when implementation or review discovers a
new decision, resolves an unanswered question, or deliberately changes an initial
assumption. Record it in the same pull request as the code it affects.

## Commits

Use Conventional Commits with an imperative subject and no trailing period. Wrap body
lines at 72 columns and explain why when the rationale is not obvious from the diff.

```text
feat(catalog): filter food items by multiple categories

Selections are additive, while an empty selection means no filter.

Refs #4
```

Allowed types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf`.
Use `Refs #N` in commits and `Closes #N` in the pull request. The configured `commit-msg`
hook validates the conventional subject and line lengths; review enforces rationale and
issue traceability.

## Definition of done

- The issue's acceptance criteria are met.
- Changed domain and state logic has meaningful tests.
- User-visible behavior has automated evidence where valuable and a manual emulator check
  when visual or interaction judgment is required.
- Every criterion maps to test, build, document, or manual evidence.
- New decisions or deviations are recorded without duplicating initial assumptions.
- `./gradlew verify` is green.
- The diff contains no unrelated work and commits reference the issue.
