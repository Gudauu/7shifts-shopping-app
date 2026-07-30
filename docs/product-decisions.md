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
- **Category overflow has functional navigation in addition to its fade** (Issue #10,
  user review). The fade alone did not make sideways scrolling discoverable enough. A
  shopper can still swipe the single-row chips, but visible previous and next controls
  now move the row through its pages and carry meaningful accessibility labels.

## Cart

<!-- New decisions or deviations from Issues #5–#7 assumptions. -->

- **The badge disappears at zero rather than showing 0** (Issue #5, emulator check).
  The initial assumptions cover what the badge counts but not what an empty cart looks
  like. A permanently visible "0" reads as something needing attention, so the "View
  cart" action shows no badge until the first add, and the badge itself is the
  confirmation that an add landed. The standalone badge beside the button text, rather
  than a `BadgedBox` overlay that the button's clipping could cut, renders as an intact
  pill on a phone-sized emulator; Robolectric resolves the badge text on the merged
  button node, so that look is emulator evidence, not test evidence. The badge is blue
  rather than Material's default error red, because the count is neutral information
  and red reads as an alert. After Issue #7 made per-card quantities plain numbers, the
  merged button gained an explicit `N items in cart` state description, keeping badge
  assertions and accessibility semantics distinct from an identical card count.
- **The cart badge uses the checkout theme accent** (Issue #10, user review). This
  supersedes the Issue #5 choice of informational blue: the count now uses the same
  primary purple as the checkout button so the two stages of the journey feel related.
- **The cart stores lines of item and quantity, keyed by uuid** (Issue #5). The badge
  only needs a total, but re-adding an item has to register somewhere, and a line with a
  quantity is the direct representation of "the same item can be added repeatedly". It
  also preserves first-added order, which issue #6 assumes for the cart rows, without
  the cart screen having to aggregate raw adds itself.
- **The cart badge is screen-level state, not catalog content state** (Issue #5,
  review). The badge count sits beside the catalog section in the UI state rather than
  inside the loaded content, so a reload cannot blank a non-empty badge while the list
  is away. No reload path exists after the first success today, but pull-to-refresh or
  a post-purchase refresh in #8 must not flash the cart to zero mid-load.
- **Cart lines snapshot the item at add time** (Issue #5, review). A line keeps the
  `FoodItem` it was added with, price included. Within a session the catalog is fetched
  exactly once, so a line cannot diverge from the catalog today, and the #8 contract
  makes server-side pricing authoritative at purchase, so the snapshot is display data
  rather than a price promise. Revisit if a catalog reload path ever arrives.
- **Large-font-scale layout is emulator-checked, not Robolectric-checked** (Issue #5,
  review). The card's price takes layout weight so an enlarged font wraps the price
  rather than pushing the add control off the card. Robolectric cannot regress-test
  this: its text measures around a pixel per character regardless of font scale, so no
  text can ever crowd the row on the JVM. The check belongs to the manual emulator pass
  alongside the badge's visual placement.
- **Each card shows its own in-cart quantity next to a green add sign** (Issue #5,
  review; emulator check). The badge answers "how much in total", but while browsing the
  useful question is "how many of this one do I already have", so a card shows its count
  beside the add control once the item is in the cart and nothing before. The labeled
  "Add" button became a green plus icon drawn in-app, because the pinned material3 no
  longer brings material-icons along and an icon set is not worth a dependency of its
  own. The quantity and the plus sit together in a translucent light-green pill, so they
  read as one control rather than two stray glyphs. The Issue #7 follow-up removed the
  multiplication prefix, giving catalog and cart the same plain count followed by an
  action icon; checked on the emulator in light and dark.
- **Cart quantity controls group the count with the decrease action** (Issue #7,
  follow-up; emulator check). Moving quantity out of the unit-price label makes the
  adjustment easier to scan. The cart uses a soft coral decrease pill, and swaps the
  minus for an empty-outline bin at quantity 1 to signal that the next tap removes the
  row. The softer palette won an emulator comparison over vivid red and muted berry:
  decreasing a quantity is routine, so it should not read as a high-severity alert. Unit
  and line prices share the lower row in `unit / total` order; checked in light and dark
  themes and at large font scale.
- **The order total stays pinned below the scrolling rows** (Issue #6, emulator check).
  The issue lists the rows and an order total but not where the total lives. The rows
  scroll while the total sits under a divider at the bottom of the screen, so what the
  order costs stays visible however long the cart grows. Checked on a phone-sized
  emulator in light and dark, together with the empty state, a mixed cart's line and
  order totals, and the cart surviving the activity recreation that the theme switch
  forces.
- **Cart rows carry a thumbnail and sit in their own card** (Issue #6, review). Plain
  text rows were hard to scan, so each row shows the item's image on the left and sits
  in the same card surface the catalog's items use, which separates the rows without
  needing dividers. A missing image falls back to the same placeholder as the catalog
  card rather than collapsing the row; checked on the emulator in light and dark.

## Purchase

<!-- New decisions or deviations from Issue #8 assumptions and contract design. -->

- **A purchase attempt is readable for uncertain-outcome recovery** (Issue #8, contract
  review). `POST /api/purchases` now identifies a matching
  `GET /api/purchases/{purchase_uuid}` resource on success and while a duplicate request
  is still processing. This narrows the initial assumption that no order record is read
  back: there is still no order-history or collection endpoint, but the server retains
  one addressable attempt for at least the idempotency window so the client can resolve
  a lost or delayed response without risking a second purchase.
- **Purchase requests carry a CAD quote for verification** (Issue #8, contract review).
  The request requires the last displayed unit price and `CAD`, resolving the initial
  assumption that prices may be sent. The server still reloads current prices and owns
  every line total; a changed quote requires shopper review and a new logical attempt.
- **Purchase recovery stays behind the repository boundary** (Issue #8, contract review).
  The data implementation owns idempotency keys, safe retries, and polling. The domain
  sees a completed purchase or typed terminal failure, while the UI remains in one
  in-flight state throughout recovery.
- **An in-flight purchase freezes cart changes but not navigation** (Issue #8, review).
  Quantity controls stay disabled while the repository is submitting or recovering an
  immutable request snapshot. The shopper can still leave a hanging operation; doing so
  cancels local recovery, preserves the cart, and records an unresolved outcome rather
  than allowing a potentially duplicative new purchase.
- **Retry follows the contract's safety signal** (Issue #8, review). The acceptance
  criterion's retry is shown for failures where an unchanged retry is safe. Item,
  validation, unresolved-outcome, and client-state failures instead show actionable
  detail with purchase disabled; changing the cart clears correctable item or validation
  failures, while an unresolved outcome remains blocked to avoid a duplicate charge.
- **Success waits for explicit acknowledgement** (Issue #8, emulator check). The completed
  cart is replaced by a confirmation showing the authoritative total and a "Continue
  shopping" action. It does not disappear on a timer, so the result remains perceivable;
  continuing or going back clears the transient confirmation and returns to the catalog.

## Polish

- **Landscape starts with catalog controls collapsed behind a stateful summary** (Issue
  #10, user review). A compact row reports the current sort and selected-category count,
  and expands on demand. This keeps sorting and filtering reachable without consuming
  the limited vertical viewport before the product grid.
