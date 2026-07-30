# Issue breakdown

The product requirements refined into the issues I would take into a sprint, in delivery
order. Each is one pull request.

## How these were sliced

**Vertical slices, one issue at a time.** Each feature issue crosses only the layers needed
to deliver a user-visible outcome. "Build the repository layer" and "build the UI" are
tasks, not issues; splitting that way leaves nothing demonstrable until the layers are
finally connected.

**Explicit non-feature work.** Issue #1 is an enabler: a greenfield app needs a build,
formatter, CI gate, and navigable shell before a feature can land in a reviewable slice.
Issue #9 is the required retrospective platform-feedback deliverable. Neither is presented
as user-facing feature work.

**Sizing rule.** Split an issue when its acceptance criteria form unrelated clusters or
cannot be reviewed as one coherent outcome. Each issue below is one reviewable pull
request.

**Assumptions are local.** Each initial assumption is recorded against the issue where the
decision is forced, rather than collected in a global list. New decisions or deviations
discovered during implementation are recorded in `docs/product-decisions.md` without
duplicating the initial assumption. The API leaves real gaps (no stock, no quantity,
prices as JSON floats, categories joined by uuid), and where those gaps are resolved is
more informative than that they exist.

## What the API actually returns

Both endpoints return a flat JSON array with no envelope, no pagination, and no auth.

`GET /api/food_items.json` — 30 items:

```json
{
  "uuid": "a1f7b3e5-4c1d-42e9-8f2a-8cbb8b1f6f01",
  "name": "Bananas",
  "price": 1.49,
  "category_uuid": "b1f6d8a5-0e29-4d70-8d4f-1f8c1d7a5b12",
  "image_url": "https://7shifts.github.io/mobile-takehome/images/bananas.png"
}
```

`GET /api/food_item_categories.json` — 6 categories (Produce, Meat, Dairy, Bakery,
Frozen, Pantry):

```json
{ "uuid": "b1f6d8a5-0e29-4d70-8d4f-1f8c1d7a5b12", "name": "Produce" }
```

Notably absent: stock or availability, any quantity concept, a display order, and any
purchase endpoint.

## Delivery order

| # | Issue | Type |
| --- | --- | --- |
| 1 | Project scaffold and walking skeleton | Enabler |
| 2 | Browse the food items | Slice |
| 3 | Sort food items by price | Slice |
| 4 | Filter food items by one or more categories | Slice |
| 5 | Add an item to the cart and see the cart count | Slice |
| 6 | View the cart | Slice |
| 7 | Remove an item from the cart | Slice |
| 8 | Design the purchase contract and purchase the cart | Slice + deliverable |
| 9 | Platform feedback | Deliverable |
| 10 | Polish the end-to-end shopping experience | Polish |

---

## Issue #1: Project scaffold and walking skeleton

**Type:** Enabler. No user-facing behaviour.

**Why it exists**
Every later issue needs a build that compiles, a formatter that stops style drift, a gate
that runs on every change, and a navigable two-screen shell to hang features on. Doing
this once up front means issues #2 through #8 are pure feature work.

**Scope**
- Gradle project on AGP 8.13, Kotlin 2.2, JDK 17, compileSdk 36, minSdk 26.
- Spotless with ktlint and the Compose rule set, plus an `.editorconfig`.
- `verify` and `verifyCi` Gradle tasks, and a GitHub Actions workflow running `verifyCi`.
- Hilt wired end to end: annotated `Application`, `@AndroidEntryPoint` activity.
- Type-safe Navigation Compose shell with two destinations, catalog and cart.
- Package skeleton and the dependency rule documented in `AGENTS.md`.
- `commit-msg` hook enforcing the commit convention.

**Acceptance criteria**
- [ ] `./gradlew verify` passes from a clean checkout.
- [ ] CI runs `verifyCi` on pull requests and reports a status check.
- [ ] The app launches on a phone-sized emulator and shows the food items screen.
- [ ] The cart screen is reachable and returns to the food items screen.

**Out of scope**
- Any real data. Both screens are placeholders.
- Networking, repositories, DTOs, and view models. Those arrive with issue #2.

**Assumptions**
- minSdk 26 is acceptable. It covers effectively the whole active device base and avoids
  desugaring work for `java.time` and `BigDecimal` formatting.
- Single module. At eight feature issues the coordination cost of modularization exceeds
  its benefit; the layer boundaries are enforced by package and by review instead. This is
  a deliberate deferral and is revisited in `platform_feedback.md`.
- Hilt rather than manual dependency injection. It is heavier than this app strictly
  needs, but it is what an Android team is likely to already run, and the graph will grow
  once repositories and view models arrive.
- Dynamic colour is disabled so the app renders identically on every device, which keeps
  per-issue screenshots comparable.
- The dependency set is capped at what compileSdk 36 and AGP 8.13 support. The current
  androidx releases require compileSdk 37 and AGP 9.1, which is a major-version jump not
  worth taking mid-exercise. Android Lint therefore reports version-availability warnings
  by design; the reasoning is recorded in `AGENTS.md` so it is not "fixed" by accident.

**Test plan**
- Robolectric Compose test: the app opens on the food items screen, navigates to the
  cart, and returns. This also proves the JVM UI test harness works, which every later
  issue depends on.

---

## Issue #2: Browse the food items

**User value**
As a shopper, I want to see all the food items with their name, price, category, and
picture, so I can decide what I want to buy.

**Context:** `docs/product-decisions.md#browse`

**Scope**
- Fetch items from `GET /api/food_items.json` and categories from
  `GET /api/food_item_categories.json`, and join them on `category_uuid`.
- Repository interface in `domain`, Retrofit implementation in `data`, DTO to model
  mapping at the boundary.
- Grid of cards showing name, formatted price, category name, and image.
- Loading, error with retry, and empty states.

**Acceptance criteria**
- [ ] All 30 items render with name, price, category name, and image.
- [ ] Prices display as `$1.49`, always two decimals.
- [ ] A spinner shows while loading and is replaced by the list.
- [ ] A network failure shows an error state with a working retry.
- [ ] State survives a configuration change without refetching.

**Out of scope**
- Sorting (#3), filtering (#4), adding to the cart (#5).
- Item detail screens. The requirements describe a list, not a detail view.
- Caching, offline support, and pagination.

**Assumptions**
- **Prices are money, so they are `BigDecimal`, not `Double`.** The API sends JSON floats,
  which cannot represent `1.49` exactly; parsing straight to `Double` produces visible
  cent errors once the cart starts summing them. The DTO parses to `BigDecimal` and the
  UI formats at the edge.
- The two endpoints are fetched independently and joined on device. There is no
  guaranteed consistency between them. Their failure mode is all-or-nothing: 
  if either endpoint fails, the screen shows the error state rather than a partial 
  catalog, because a retry almost always fixes both.
- `uuid` values identify the item. When two elements share a uuid, the later
  element overrides the earlier one; the grid keys on the uuid, and later issues rely
  on it as the cart identity.
- When `category_uuid` matches no category, it is shown with no category 
  label rather than being hidden or crashing. Losing a purchasable item because of a metadata
  gap is the worse failure.
- Images are remote PNGs loaded by Coil, with a placeholder while loading and on failure.
  A missing image must not hide the item.
- An item is shown only if it decodes, its `uuid` and `name` are non-blank, and its
  `price` parses to a non-negative amount. `category_uuid` and `image_url` remain
  optional.
- A category is used only if it decodes with a non-blank `uuid` and `name`; a dropped
  category leaves its items visible without a label. Duplicate category uuids also
  resolve to the later element.
- 30 items with no pagination in the response means the whole list loads at once. No
  paging.

**Test plan**
- Unit: DTO to domain mapping, including the price landing as an exact `BigDecimal`.
- Unit: joining items to categories, including the unmatched `category_uuid` case.
- Unit: state holder emits loading, then content; and loading, then error, then content
  after retry.
- Robolectric: items render, and the error state shows a retry that recovers.

---

## Issue #3: Sort food items by price

**User value**
As a shopper on a budget, I want to order the list by price, so I can find the cheapest
or most expensive options quickly.

**Context:** `docs/product-decisions.md#sort`

**Scope**
- A control on the food items screen to sort by price ascending or descending.
- Sorting applies to the currently displayed list and is done on device.

**Acceptance criteria**
- [ ] Ascending shows the lowest price first.
- [ ] Descending shows the highest price first.
- [ ] The active sort is visible in the UI.
- [ ] The sort survives a configuration change.

**Out of scope**
- Sorting by name, category, or anything else. Not in the requirements.
- Server-side sorting. The endpoint is a static file.

**Assumptions**
- The API's array order is the default until the user chooses a sort. It is treated as
  intentional rather than re-ordered on arrival.
- Items with equal prices keep their relative API order, so sorting is stable and the
  list does not shuffle when the user toggles direction.
- Sort is a single choice, not a multi-key sort.

**Test plan**
- Unit: ascending and descending produce the expected order over a fixed list.
- Unit: equal prices preserve relative order in both directions.
- Robolectric: toggling the control reorders the rendered list.

---

## Issue #4: Filter food items by one or more categories

**User value**
As a shopper, I want to narrow the list to the categories I care about, so I can find
what I need without scrolling all 30 items.

**Context:** `docs/product-decisions.md#filter`

**Scope**
- Multi-select category filter on the food items screen.
- Filtering is additive: selecting Produce and Dairy shows both.
- Zero selections means no filter.
- Filter composes with the sort from #3; neither resets the other.
- Empty state when a selection matches nothing.

**Acceptance criteria**
- [ ] Selecting one category shows only that category's items.
- [ ] Selecting two categories shows the union of both.
- [ ] Deselecting everything restores the full list.
- [ ] Applying a sort and then a filter keeps both applied.
- [ ] The selection survives a configuration change.

**Out of scope**
- Search by name, price-range filters, dietary filters.
- Persisting the selection across app launches.

**Assumptions**
- Chips render in the order the categories endpoint returns, which is treated as
  deliberate rather than alphabetized.
- Filter is a union (OR) across categories, not an intersection. An item has exactly one
  category, so an intersection of two categories would always be empty.
- Items with an unresolvable `category_uuid` are excluded whenever any filter is active,
  since they belong to no selected category, but remain visible when no filter is set.
- Six categories is few enough to render as inline chips rather than a bottom sheet.

**Test plan**
- Unit: filtering over none, one, many, and no-match selections.
- Unit: filter and sort applied in either order produce the same list.
- Robolectric: selecting a chip updates the list; a no-match selection shows the empty
  state.

---

## Issue #5: Add items to the cart and see the cart count

**User value**
As a shopper, I want to add items to my cart and see how many I have, so I can build up
an order while browsing.

**Context:** `docs/product-decisions.md#cart`

**Scope**
- An add control on each item card.
- Cart state in `domain`, shared across screens.
- A badge on the food items screen showing the total number of items.
- The same item can be added repeatedly.

**Acceptance criteria**
- [ ] Adding an item increments the badge.
- [ ] Adding the same item three times shows a count of 3.
- [ ] The badge shows the total item count, not the number of distinct items.
- [ ] The cart survives a configuration change.

**Out of scope**
- Viewing (#6), removing (#7), purchasing (#8).
- Persisting the cart across app launches.

**Assumptions**
- **The cart is in memory only.** The requirements describe no persistence, and a cart
  that survives a cold start would need an invalidation policy against a catalogue that
  can change. Called out explicitly because it is the assumption most likely to be
  challenged.
- "Total number of items in their cart" counts every add, so three bananas is 3 rather
  than 1. That reading matches the requirement that the same item can be added multiple
  times.
- No stock limits, so there is no maximum quantity. The API exposes no availability.
- Adding is silent, with no confirmation dialog. The badge is the feedback.

**Test plan**
- Unit: adding distinct items, adding duplicates, and the resulting total count.
- Unit: the cart is empty on construction.
- Robolectric: tapping add updates the badge; three taps on one item shows 3.

---

## Issue #6: View the cart

**User value**
As a shopper, I want to see what is in my cart and what it costs, so I can check my order
before buying.

**Context:** `docs/product-decisions.md#cart`

**Scope**
- Cart screen listing each distinct item with its quantity, unit price, and line total.
- Order total.
- Empty cart state.

**Acceptance criteria**
- [ ] Each distinct item appears once, with its quantity.
- [ ] The line total equals unit price times quantity.
- [ ] The order total equals the sum of the line totals.
- [ ] An empty cart shows an empty state rather than a blank screen.

**Out of scope**
- Editing quantities inline, removing (#7), purchasing (#8).
- Taxes, tips, discounts, delivery fees. None are in the requirements.

**Assumptions**
- Duplicates collapse into one row with a quantity rather than repeating as N rows. The
  API has no quantity concept, so this aggregation is purely a client-side display
  decision.
- Rows are ordered by when the item was first added, so the list does not reorder as
  quantities change.
- Totals are computed in `BigDecimal` and rounded once, at display time, using
  `HALF_UP`. Rounding each line before summing would drift.

**Test plan**
- Unit: line totals and order total over a mixed cart, asserting exact `BigDecimal`
  values.
- Unit: an empty cart shows "Your cart is empty".
- Robolectric: a cart with a duplicated item renders one row with quantity 2 and the
  correct totals.

---

## Issue #7: Remove items from the cart

**User value**
As a shopper, I want to take something out of my cart, so I can correct a mistake without
starting over.

**Context:** `docs/product-decisions.md#cart`

**Scope**
- A quantity decrease control on each item row on the cart screen.
- Each tap removes one unit of the item.
- Totals and the badge update immediately.

**Acceptance criteria**
- [ ] Decreasing an item with a quantity greater than 1 removes one unit and keeps the
  row visible with the updated quantity.
- [ ] Decreasing an item with a quantity of 1 removes its final unit and deletes the row.
- [ ] The order total and the cart badge both decrease by one unit.
- [ ] Removing the final unit from the cart shows the empty cart state.

**Out of scope**
- Removing a whole line in one action, increasing quantity inline, undo, swipe-to-delete.

**Assumptions**
- **Removal happens one unit at a time.** The decrease control lets shoppers correct the
  quantity without removing every unit of an item. At quantity 1, the same control
  removes the final unit and its row.
- No confirmation dialog. The action is cheap to reverse by re-adding.

**Test plan**
- Unit: decreasing a line with multiple units reduces its quantity, total count, and
  order total by one unit.
- Unit: decreasing a line at quantity 1 removes it; removing the final cart unit leaves
  the cart empty.
- Robolectric: tapping decrease updates the rendered quantity, totals, and badge, then
  removes the row when its final unit is removed.

---

## Issue #8: Design the purchase contract and purchase the cart

**User value**
As a shopper, I want to buy everything in my cart, so I can complete my order.

**Context:** `docs/product-decisions.md#purchase`

**Scope**
- Design and document `purchase_api_contract.md` before implementing the client boundary.
  The contract covers the endpoint and method, an idempotency key, item UUIDs and
  quantities, server-authoritative pricing and totals, success data, per-item failures,
  and an error shape the UI can act on.
- A purchase button on the cart screen.
- A `PurchaseRepository` interface in `domain` matching the contract proposed in
  `purchase_api_contract.md`, with a stub implementation, since the endpoint does not
  exist.
- In-flight, success, and failure states.
- The cart empties on success only.

**Acceptance criteria**
- [ ] `purchase_api_contract.md` specifies the request, success response, and actionable
  error responses with example JSON.
- [ ] The request supports idempotent retries and identifies each item and quantity without
  treating client-provided prices as authoritative.
- [ ] `PurchaseRepository` and its stub align with the documented contract.
- [ ] Purchasing an empty cart is not possible; the button is disabled.
- [ ] The button shows progress and cannot be double-submitted while in flight.
- [ ] On success the cart empties and a confirmation is shown.
- [ ] On failure the cart is preserved and an error with a retry is shown.

**Out of scope**
- Payment, addresses, order history, receipts.
- A real network call. The endpoint is unimplemented.

**Assumptions**
- **The endpoint does not exist, so the boundary is an interface with a stub behind it.**
  Swapping in a real implementation should be a one-file change. Designing the interface
  from the proposed contract is the point of the exercise.
- The mobile client is the only known consumer today, but the contract does not assume it
  will remain the only consumer.
- Item prices may be sent for verification or display context, but the server recomputes
  authoritative prices and totals.
- Failure preserves the cart. Emptying it on a failed purchase would lose the user's work
  with no way to recover it.
- Double submission is prevented in the state holder rather than by disabling the button
  alone, since a fast double tap can land two clicks before recomposition.
- Success is terminal for this iteration: a confirmation, then back to browsing. No order
  record, because nothing in the requirements reads one back.

**Test plan**
- Document inspection: request and response examples are internally consistent and cover
  success, validation failure, unavailable items, idempotent replay, processing recovery
  to completed or failed, and the correct key choice after a terminal failure.
- Unit: purchase succeeds, cart empties; purchase fails, cart is preserved.
- Unit: a second purchase call while one is in flight is ignored.
- Robolectric: the button is disabled when the cart is empty, and the cart shows its
  empty state after a successful purchase.

---

## Issue #9: Platform feedback

**Deliverable:** `platform_feedback.md`

**Scope**
What I would standardize or abstract next, written from what actually cost time across
issues #1 to #8 rather than from a generic best-practices list. Drawn from
`docs/product-decisions.md`, which accumulated the new decisions and deviations discovered
after the initial issue assumptions.

Expected themes: a shared UI state and error-with-retry pattern instead of each screen
re-deriving one, a money type so `BigDecimal` handling is not repeated per feature, a
convention plugin owning the Gradle gate once a second module exists, screenshot testing
to close the visual gap that Robolectric cannot cover, and the module split that was
deliberately deferred in issue #1.

---

## Issue #10: Polish the end-to-end shopping experience

**User value**
As a shopper, I want the complete browsing, cart, and purchase journey to feel consistent
and clear, so I can move through it without visual or interaction friction.

**Scope**
- Audit the integrated journey on a phone-sized emulator after issues #8 and #9 land:
  browse, sort, filter, add, adjust quantities, and purchase through both success and
  failure outcomes.
- Fix concrete visual and interaction inconsistencies found across the catalog, cart, and
  purchase states, including spacing, hierarchy, control treatment, feedback, and copy.
- Verify accessibility semantics, touch targets, and layouts at a large font scale.
- Remove dead UI code, stale comments or copy, and obsolete one-off styling directly
  exposed by the polish work. Unrelated refactoring remains out of scope.
- Add or update focused tests when a changed interaction or semantic contract can regress
  meaningfully on the JVM.

**Acceptance criteria**
- [ ] The full browse-to-purchase journey has no clipped, overlapping, unreachable, or
  visually ambiguous controls on a phone-sized emulator at default and large font scales.
- [ ] Catalog, cart, and purchase states use a consistent visual hierarchy, spacing,
  control language, and user-facing terminology.
- [ ] Icon-only actions have meaningful accessibility labels, and controls with changing
  values expose their current state.
- [ ] Loading, empty, error, submitting, success, and failure states clearly communicate
  what happened and the next available action.
- [ ] The journey is manually checked in light and dark themes, and the findings and final
  evidence are recorded in the pull request.
- [ ] `./gradlew verify` passes.

**Out of scope**
- New shopping capabilities, screens, or changes to the purchase contract.
- Dependency upgrades, new visual-testing infrastructure, or broad architectural
  refactoring.
- Cleanup in code unaffected by an observed polish finding.

**Assumptions**
- The implementation branch is updated from `main` after issues #8 and #9 merge, so the
  audit covers the complete integrated app rather than an intermediate feature branch.
- The emulator audit determines the concrete fixes; this issue is not a speculative
  redesign or an excuse to restyle already coherent UI.
- Existing domain behavior and state contracts remain unchanged unless an observed UX
  defect requires a narrowly documented correction.

**Test plan**
- Focused Robolectric Compose tests for changed semantics and interactions.
- Existing unit and Compose suites through `./gradlew verify`.
- Manual end-to-end emulator pass at default and large font scales in light and dark
  themes, covering catalog loading and failure plus purchase success and failure.
