# Product decisions

This is a change log for feature-scoped decisions discovered after planning. Initial
assumptions remain with their issue in `issue_breakdown.md`; do not duplicate them here.
Cross-cutting engineering rules belong in `AGENTS.md`.

Each issue names the section it depends on, for example
`Context: docs/product-decisions.md#filter`. Append an entry only when implementation or
review discovers a new decision, resolves an unanswered question, or deliberately changes
an initial assumption. Record the issue and rationale in the same pull request. A recorded
deviation supersedes the corresponding initial assumption and becomes raw material for
`platform_feedback.md`.

## Browse

<!-- New decisions or deviations from Issue #2 assumptions. -->

- **Catalog state is activity-scoped, not destination-scoped** (Issue #2). The catalog
  view model is created once per activity and passed into the nav host, rather than by a
  `hiltViewModel()` call inside the catalog destination. Beyond the required
  configuration-change survival, this means navigating to the cart and back never
  refetches, and the Robolectric harness can drive the nav host with a fake repository
  without any Hilt test infrastructure, which would otherwise need a new test dependency.

## Sort

<!-- New decisions or deviations from Issue #3 assumptions. -->

## Filter

<!-- New decisions or deviations from Issue #4 assumptions. -->

## Cart

<!-- New decisions or deviations from Issues #5–#7 assumptions. -->

## Purchase

<!-- New decisions or deviations from Issue #8 assumptions and contract design. -->
