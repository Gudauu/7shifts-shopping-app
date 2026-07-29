package com.sevenshifts.shopping.data.network

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray

private const val TAG = "ShoppingApi"

/**
 * Decodes each element with [serializer], dropping elements that fail instead of letting
 * one malformed element fail the whole payload. Retry heals transport failures, not data
 * failures, so an undecodable element is logged and hidden rather than error-screening
 * the catalog.
 */
internal fun <T : Any> JsonArray.decodeValidElements(serializer: KSerializer<T>): List<T> = mapNotNull { element ->
    try {
        shoppingJson.decodeFromJsonElement(serializer, element)
    } catch (e: SerializationException) {
        Log.w(TAG, "Dropping element that failed to decode: $element", e)
        null
    } catch (e: IllegalArgumentException) {
        // BigDecimal parsing throws NumberFormatException for garbage price literals.
        Log.w(TAG, "Dropping element that failed to decode: $element", e)
        null
    }
}
