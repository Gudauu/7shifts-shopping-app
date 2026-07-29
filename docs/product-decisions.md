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

- **Absent metadata keys are tolerated, not fatal** (Issue #2, review). `image_url` and
  `category_uuid` are optional in the DTO. kotlinx.serialization fails the whole array
  over one missing required field, so a single item without an image key would have
  error-screened all 30 items with a retry that can never succeed. This reads the initial
  assumption "a missing image must not hide the item" as covering an absent key as well
  as a failed download. Identity fields (`uuid`, `name`, `price`) stay required, because
  an unidentifiable or unpriced item is not purchasable.
- **Data that violates the catalog assumptions is dropped, not shown and not fatal**
  (Issue #2, follow-up). Retry heals transport failures, not data failures, so the error
  state with retry is reserved for failed fetches. Payloads decode element by element:
  an element that fails to decode, an item with a blank `uuid` or `name` or a negative
  price, and a category with a blank field are dropped and logged. A duplicated uuid
  resolves to the later element, as an override. This supersedes the fail-loudly reading
  of "identity fields stay required": a missing required field now costs one element,
  never the catalog.
- **Catalog state is activity-scoped, not destination-scoped** (Issue #2). The catalog
  view model is created once per activity and passed into the nav host, rather than by a
  `hiltViewModel()` call inside the catalog destination. Beyond the required
  configuration-change survival, this means navigating to the cart and back never
  refetches, and the Robolectric harness can drive the nav host with a fake repository
  without any Hilt test infrastructure, which would otherwise need a new test dependency.

## Sort

<!-- New decisions or deviations from Issue #3 assumptions. -->

- **The active sort can be cleared** (Issue #3). The control is a pair of filter chips,
  and tapping the active chip deselects it, returning the list to the API's order. The
  initial assumption made API order the default before any choice; this makes it
  reachable again afterwards, because a chip control that can never be deselected would
  trap the user in a sort the requirements treat as optional.

## Filter

<!-- New decisions or deviations from Issue #4 assumptions. -->

## Cart

<!-- New decisions or deviations from Issues #5–#7 assumptions. -->

## Purchase

<!-- New decisions or deviations from Issue #8 assumptions and contract design. -->
