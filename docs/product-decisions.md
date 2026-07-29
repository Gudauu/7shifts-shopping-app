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

- **Applying a sort presents the top of the new order** (Issue #3, emulator check). The
  keyed lazy grid anchors scrolling to the first visible card, so a reorder made the
  viewport follow that card to its new position, usually the bottom of the list. The grid
  now scrolls back to the top when the sort changes, since the point of choosing a sort
  is to see the leading items of the new order. Restoring the screen after a
  configuration change or navigation still keeps its scroll position.
- **The active sort can be cleared** (Issue #3). The control is a pair of filter chips,
  and tapping the active chip deselects it, returning the list to the API's order. The
  initial assumption made API order the default before any choice; this makes it
  reachable again afterwards, because a chip control that can never be deselected would
  trap the user in a sort the requirements treat as optional.

## Filter

<!-- New decisions or deviations from Issue #4 assumptions. -->

- **The catalog exposes category identity, not just a display name** (Issue #4). The
  filter needs a stable identity to match on, and the chips must offer every category the
  endpoint returns, including one that no current item references. The repository
  therefore returns the category list alongside the items, and each item carries its
  resolved category (uuid and name) instead of a bare name. Matching on the uuid rather
  than the display name keeps the filter correct if two categories ever share a name.
- **The chip row signals its overflow with an edge fade** (Issue #4, review). The
  category chips scroll sideways, but a row that ends cleanly at the screen edge reads as
  the complete list, so the off-screen categories were undiscoverable. The row now fades
  into the background on any edge with more content beyond it, and the fade disappears
  once that end is reached.
- **Changing the filter presents the top of the new list** (Issue #4). The Issue #3
  decision that applying a sort scrolls back to the top applies equally to toggling a
  category chip: both replace the list, and the point of narrowing it is to see its
  leading items. Restoring the screen after a configuration change or navigation still
  keeps its scroll position.

## Cart

<!-- New decisions or deviations from Issues #5–#7 assumptions. -->

- **The badge disappears at zero rather than showing 0** (Issue #5). The initial
  assumptions cover what the badge counts but not what an empty cart looks like. A
  permanently visible "0" reads as something needing attention, so the "View cart"
  action shows no badge until the first add, and the badge itself is the confirmation
  that an add landed.
- **The cart stores lines of item and quantity, keyed by uuid** (Issue #5). The badge
  only needs a total, but re-adding an item has to register somewhere, and a line with a
  quantity is the direct representation of "the same item can be added repeatedly". It
  also preserves first-added order, which issue #6 assumes for the cart rows, without
  the cart screen having to aggregate raw adds itself.

## Purchase

<!-- New decisions or deviations from Issue #8 assumptions and contract design. -->
