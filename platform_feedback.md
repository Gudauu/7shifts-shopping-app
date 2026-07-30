# Platform feedback

This first iteration exposed three areas where the next feature would otherwise repeat work.
These are extracted from the implementation process, as well as revealed from docs/product-decisions.md.

## Standardize a state-driven screen foundation

### What repeated

The catalog and purchase flows each needed loading or operation state, actionable
failures, and rules about how long state lives. The important lessons were not the names
of their sealed classes, but the mechanics around them.

For example:

- The cart badge had to sit outside catalog content so a reload could not temporarily
  erase it.
- Purchase submission needed a state-holder guard because disabling a button could not
  stop two taps that arrived before recomposition.
- A purchase failure had to preserve the cart, while a catalog fetch failure could
  replace the catalog with a retry action.

Visual review found another repeated cost. Semantics tests proved clicks, navigation, and
state transitions, but they did not catch the clipped badge, undiscoverable category
overflow, crowded large-text layout, or weak grouping of quantity controls.

### What to standardize

Provide a small screen foundation:

- `Loadable<T>` for an independently loaded resource: loading, content, or failure.
- A UI-facing failure containing presentation copy and an optional action such as retry.
- Shared `LoadingPane`, `EmptyPane`, and `ErrorPane` composables with consistent spacing,
  accessibility behavior, and action placement.
- `ProductImage`, including the loading and missing-image fallback already duplicated by
  the catalog and cart.

Each state holder should expose one immutable `StateFlow`, enforce action eligibility in
its event methods, and keep durable content visible when a secondary action fails. Domain
and repository failures should be translated into presentation failures at the feature
boundary.

This should not become a universal `ScreenState` or base `ViewModel`. Filter selections,
cart contents, purchase success, item failures, and uncertain outcomes are feature state
and should remain explicit.

Add a screenshot-test harness for the same state-driven composables. A small,
risk-selected set is more useful than a matrix of every state and device combination.
Good first cases would be:

- A long product name at large text size on a narrow phone.
- Category chips overflowing the available width.
- A cart with multiple-digit quantities in light and dark themes.
- Purchase in-flight, failure, and success states.

Use pinned device settings, fixed local image fixtures, disabled dynamic color, and
stable animation frames. Keep semantics tests for behavior and accessibility; screenshots
cover wrapping, clipping, hierarchy, and color. A small emulator pass should remain for
gestures and platform behavior that the screenshot renderer cannot faithfully reproduce.

## Make money a domain type

### What repeated

Exact prices crossed every layer. The catalog needed a JSON-number-to-`BigDecimal`
serializer, the cart added exact multiplication and summation, the second screen caused a
shared formatter to be extracted, and purchase introduced client quotes plus
server-authoritative totals. Tests also had to remember that `BigDecimal` equality is
scale-sensitive.

`BigDecimal` prevents binary floating-point errors, but it does not prevent inconsistent
rounding, mismatched currencies, or the wrong wire representation.

### What to standardize

Introduce a domain `Money` value containing an exact amount and currency. It should own
same-currency addition, multiplication by quantity, comparison, and a canonical equality
policy.

For example:

- `Money.cad("1.49") * 3` produces exactly `Money.cad("4.47")`.
- Adding CAD and USD fails unless an explicit conversion is supplied.
- A completed purchase replaces the client's quoted total with the server-authoritative
  `Money` value.

Keep responsibilities at their boundaries:

- Domain arithmetic remains exact and does not round intermediate totals.
- Presentation follows the current product rule and formats dollars as `$0.00`.
- The catalog adapter assigns the app's configured CAD currency because the catalog API
  does not provide one.
- Data adapters own wire formats: catalog prices are JSON numbers, while the proposed
  purchase contract uses decimal strings.
- A sellable item's non-negative price remains a business rule rather than a restriction
  on every possible `Money` value.

Provide fixture builders and contract tests for parsing, arithmetic, equality, currency
mismatch, formatting, and serialization. After migration, raw `BigDecimal` should remain
inside `Money` and wire adapters rather than appearing throughout models and feature
tests.

## Standardize architecture conventions when the next split is justified

### What repeated

This iteration was intentionally kept as a single module. That made development straightforward, but architectural boundaries such as ui → domain ← data currently rely on package structure and code review rather than compiler enforcement.

At this scale, that trade-off is appropriate. Two screens do not justify the additional complexity of feature modules, but the project should have clear criteria for when that investment becomes worthwhile.

### What to standardize

Rather than modularizing immediately, I would first standardize the architectural conventions that every new feature follows:

* UI depends only on domain models and repository interfaces.
* Data implementations own networking and mapping from DTOs to domain models.
* Domain models remain platform-independent and do not depend on Android or networking libraries.
* New features follow the same package structure and testing conventions to reduce onboarding and code review overhead.

Once a second independently owned feature or measurable build-time pressure appears, I would migrate those boundaries into Gradle modules so architectural rules become compiler-enforced rather than review-enforced. At that point, feature modules can depend on shared domain interfaces without depending directly on implementation details.

Until that trigger is reached, I would keep the project as a single module. Avoiding premature modularization keeps the codebase simple while still leaving a clear migration path as the application grows.

## What I would deliberately not abstract yet

- No base repository, generic use-case layer, or universal MVI framework. The catalog
  query and purchase command have different failure and recovery semantics.
- No generic sort/filter engine. Those rules are small, pure, tested, and currently have
  one consumer.
- No reusable quantity control. Catalog add and cart decrease look related but express
  different actions and accessibility semantics.
- No shared data-quality reporter or repository error taxonomy until a real purchase
  client or second remote repository demonstrates the common shape.
- No offline cache, persisted cart, pagination, or order-history platform until product
  requirements define freshness, invalidation, identity, and recovery.

## Recommended sequence

1. Introduce `Money` before another price-bearing feature expands the migration surface.
2. Use the screen foundation on the next asynchronous screen, migrate the catalog as the
   reference, and add screenshots for the highest-risk catalog and cart states.
3. Apply the module and build conventions when the next feature or build measurements
   justify the split.

The result is concrete: money rules have one owner, new screens begin with tested state
and error patterns, visual regressions produce reviewable diffs, and future modules enter
the project verification gate by convention.
