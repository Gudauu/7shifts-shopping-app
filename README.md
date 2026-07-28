# 7shifts Shopping

An Android shopping app: browse food items, sort and filter them, add them to a cart, and
purchase the cart.

## Running it

Requires JDK 17 and the Android SDK (compileSdk 36).

```bash
./gradlew installDebug          # install on a running emulator or device
./gradlew assembleDebug         # build the APK only
```

Or open the project in Android Studio and run the `app` configuration on any phone-sized
emulator. Developed against a Pixel 7a, API 35.

## Verifying it

```bash
./gradlew verify
```

Formats the code, runs Android Lint, runs the JVM unit tests, and assembles the debug
APK. CI runs `verifyCi`, which does the same four things but fails on unformatted code
instead of fixing it.

Test report after a failure: `app/build/reports/tests/testDebugUnitTest/index.html`

## Architecture

Single Gradle module, layered by package, with the boundary enforced by convention and
review rather than by module structure:

```
com.sevenshifts.shopping
  data/      DTOs, Retrofit services, repository implementations, mappers
  domain/    Models, repository interfaces, business rules. Plain Kotlin, no Android.
  ui/        Compose screens, state holders, theme, navigation
  di/        Hilt modules
```

The dependency rule is `ui -> domain <- data`. `domain` imports nothing from Android,
`ui` never imports from `data`, and DTOs never leave `data`.

Kotlin, Jetpack Compose with Material 3, Hilt, Retrofit with kotlinx.serialization, Coil,
and type-safe Navigation Compose. Prices are `BigDecimal` end to end and are formatted
only at the Compose layer.

## Testing

Unit tests cover domain logic and state holders. User-visible behaviour is covered by
Compose UI tests running on the JVM under Robolectric, so the whole suite runs in
`./gradlew verify` without an emulator. Visual correctness is verified by hand on an
emulator at the end of every issue, since the semantics tree cannot assert it.

## Documents

- [issue_breakdown.md](issue_breakdown.md) — the requirements refined into issues, with
  scope and assumptions for each.
- `purchase_api_contract.md` — proposed request and response formats for the unimplemented
  purchase endpoint; produced with Issue #8.
- `platform_feedback.md` — what I would standardize or abstract next to make the following
  features faster to deliver; produced with Issue #9.
- [AGENTS.md](AGENTS.md) — the operating rules this repository was built under.
- [docs/product-decisions.md](docs/product-decisions.md) — new decisions or deviations
  discovered after the initial assumptions in the issue breakdown.

## How this was built

The project is delivered as one pull request per issue, with a screenshot for UI changes
and a green CI run. AI coding agents work from `AGENTS.md` and the issue's acceptance
criteria, with a read-only reviewer agent
([.claude/agents](.claude/agents/correctness-reviewer.md)) checking each branch against
its requirements before I reviewed the diff and accepted it on an emulator.
