# Purchases API

The Purchases API creates one all-or-nothing purchase from the mobile cart. There is no
order-history or collection endpoint.

All paths are relative to the shopping API host and use JSON. Authentication is outside
this exercise because the provided API has no identity model. A production API must
authenticate the caller and authorize access to each purchase.

## Create a purchase

```http
POST /api/purchases
```

The server prices every item and calculates the final total. Client prices are quote
checks only and are never authoritative.

### Request

Required headers:

```http
Content-Type: application/json
Accept: application/json
Idempotency-Key: <client-generated UUID>
```

| Field | Type | Rules |
| --- | --- | --- |
| `currency` | string | Required; `CAD`. |
| `items` | array | Required and non-empty; one entry per item UUID. |
| `items[].food_item_uuid` | UUID string | Required; must identify a catalog item. |
| `items[].quantity` | integer | Required and greater than zero. |
| `items[].expected_unit_price` | decimal string | Required; last price shown to the shopper, with two fractional digits. |

Unknown fields and duplicate item UUIDs are rejected. A corrected request is a new
logical attempt and uses a new idempotency key.

```json example
{
  "currency": "CAD",
  "items": [
    {
      "food_item_uuid": "a1f7b3e5-4c1d-42e9-8f2a-8cbb8b1f6f01",
      "quantity": 2,
      "expected_unit_price": "1.49"
    },
    {
      "food_item_uuid": "e9f2c6d5-4b3e-41a7-8c4d-5e9f7a2b4a09",
      "quantity": 1,
      "expected_unit_price": "0.99"
    }
  ]
}
```

### Success

Under normal operation, the POST waits for a terminal result; it does not return a
`processing` resource. A completed purchase returns `201 Created`. `Location` identifies
the recovery resource for this attempt.

```http
HTTP/1.1 201 Created
Content-Type: application/json
Location: /api/purchases/<server-generated-purchase-uuid>
```

```json example
{
  "purchase_uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "completed",
  "created_at": "2026-07-29T22:42:16Z",
  "purchased_at": "2026-07-29T22:42:17Z",
  "currency": "CAD",
  "items": [
    {
      "food_item_uuid": "a1f7b3e5-4c1d-42e9-8f2a-8cbb8b1f6f01",
      "name": "Bananas",
      "quantity": 2,
      "unit_price": "1.49",
      "line_total": "2.98"
    },
    {
      "food_item_uuid": "e9f2c6d5-4b3e-41a7-8c4d-5e9f7a2b4a09",
      "name": "Apple",
      "quantity": 1,
      "unit_price": "0.99",
      "line_total": "0.99"
    }
  ],
  "total": "3.97"
}
```

All response prices are server-authoritative decimal strings with exactly two fractional
digits. `total` equals the sum of the line totals.

## Idempotency and uncertain outcomes

The client creates one idempotency key for a logical attempt and reuses the same key and
body after a timeout, lost response, rate limit, or retryable server error. The server
retains the key and result for at least 24 hours.

The server compares the meaning of requests, not their raw JSON bytes. Member order,
whitespace, and item order do not matter. Changing the currency, items, quantity, or
expected price does matter.

| Reuse | Result |
| --- | --- |
| Same key and request after completion | Replay the original `201` response with `Idempotency-Replayed: true`. |
| Same key and request while processing | Return `409 request_in_progress` with the purchase URI. |
| Same key and request after terminal failure | Return `409 purchase_failed` with the purchase URI. |
| Same key with a changed request | Return `409 idempotency_key_reused`; create nothing. |

The server reserves the key before purchase work and guarantees that retrying cannot
create a second purchase.

An idempotent replay returns the original status and body:

```http
HTTP/1.1 201 Created
Location: /api/purchases/<server-generated-purchase-uuid>
Idempotency-Replayed: true
```

If the first response is uncertain and the retry finds work in progress:

```http
HTTP/1.1 409 Conflict
Location: /api/purchases/<server-generated-purchase-uuid>
Retry-After: 2
```

```json example
{
  "error": {
    "code": "request_in_progress",
    "message": "The purchase is still processing.",
    "retryable": true,
    "request_id": "req_01J40K3Y7H6M2Q9B5P8N4T1VXC",
    "purchase_uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "status_url": "/api/purchases/f47ac10b-58cc-4372-a567-0e02b2c3d479"
  }
}
```

## Retrieve an uncertain purchase

```http
GET /api/purchases/{purchase_uuid}
```

This endpoint resolves an accepted attempt after the POST result was uncertain. It is not
an order-history endpoint. It returns `200 OK`, `Cache-Control: no-store`, and one of:

- `processing`, with `Retry-After` and a `resolution_deadline`;
- `completed`, with the same shape as the POST success response; or
- `failed`, confirming that no item was purchased.

```json example
{
  "purchase_uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "processing",
  "created_at": "2026-07-29T22:42:16Z",
  "resolution_deadline": "2026-07-29T22:43:16Z"
}
```

A terminal failure uses a distinct code so it cannot be confused with a retryable 503
that occurred before an attempt reached a terminal result:

```json example
{
  "purchase_uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "failed",
  "created_at": "2026-07-29T22:42:16Z",
  "error": {
    "code": "purchase_not_completed",
    "message": "The purchase could not be completed.",
    "retryable": true,
    "request_id": "req_01J40M07K3C8V6F4R2S9X5NPBQ"
  }
}
```

When a failed resource is retryable, the client starts a new logical attempt with a new
idempotency key. The server resolves every processing attempt to completed or failed by
its deadline and retains the resource for at least the idempotency window.

## Errors

All endpoint errors use the same envelope. Clients branch on `code`, not `message`.

```json example
{
  "error": {
    "code": "invalid_request",
    "message": "The purchase request is invalid.",
    "retryable": false,
    "request_id": "req_01J40K3Y7H6M2Q9B5P8N4T1VXC",
    "field_errors": [
      {
        "path": "items",
        "code": "must_not_be_empty",
        "message": "Add at least one item."
      }
    ]
  }
}
```

| HTTP | Code | Client action |
| --- | --- | --- |
| `400` | `bad_request` | Fix malformed JSON or the idempotency key. |
| `404` | `purchase_not_found` | Stop recovery, preserve the cart, and show an unresolved-purchase error. |
| `409` | `items_require_attention` | Apply shopper-approved corrections and use a new key. |
| `409` | `idempotency_key_reused` | Preserve the cart and investigate client state. |
| `409` | `purchase_failed` | Retrieve the failed resource; use a new key only if it is retryable. |
| `409` | `request_in_progress` | Retrieve `status_url` after `Retry-After`. |
| `422` | `invalid_request` | Correct `field_errors` and use a new key. |
| `429` | `rate_limited` | Honor `Retry-After`; retry the same body and key. |
| `500` | `internal_error` | Retry the same body and key. |
| `503` | `temporarily_unavailable` | Honor `Retry-After`; retry the same body and key. |

Pre-acceptance `429`, `500`, and `503` responses do not include `purchase_uuid` or
`status_url`; reusing the key is safe. An accepted terminal failure is instead represented
by `purchase_failed` and the failed resource above.

### Item errors

The server returns every affected line and creates no purchase:

```json example
{
  "error": {
    "code": "items_require_attention",
    "message": "Some items changed before purchase.",
    "retryable": false,
    "request_id": "req_01J40M07K3C8V6F4R2S9X5NPBQ",
    "item_errors": [
      {
        "food_item_uuid": "a1f7b3e5-4c1d-42e9-8f2a-8cbb8b1f6f01",
        "code": "price_changed",
        "expected_unit_price": "1.49",
        "current_unit_price": "1.59",
        "currency": "CAD",
        "message": "Bananas now cost $1.59."
      },
      {
        "food_item_uuid": "e9f2c6d5-4b3e-41a7-8c4d-5e9f7a2b4a09",
        "code": "unavailable",
        "message": "Apple is no longer available."
      }
    ]
  }
}
```

Supported item codes are `not_found`, `unavailable`, `quantity_unavailable`, and
`price_changed`. Item errors identify the item and include the current price or available
quantity when relevant.

No error creates a partial purchase. The cart is cleared only after a valid `completed`
response.

## Mobile boundary

`PurchaseRepository` accepts domain cart lines and returns a completed purchase or a
typed terminal failure. Its implementation owns idempotency keys, safe POST retries, and
status polling, so HTTP DTOs and recovery details do not leak into the domain or UI.

The ViewModel exposes one in-flight state for the entire operation, including recovery,
and rejects a second submission while that state is active. It clears the cart only after
completion; failures preserve the cart and expose an actionable message or retry.
