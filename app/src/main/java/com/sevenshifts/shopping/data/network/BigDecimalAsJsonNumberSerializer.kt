package com.sevenshifts.shopping.data.network

import java.math.BigDecimal
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses a JSON number into a [BigDecimal] from its raw literal text. Going through
 * [Double] instead would be lossy: `1.49` has no exact binary representation, and the
 * error becomes visible cents once prices are summed.
 */
object BigDecimalAsJsonNumberSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.sevenshifts.shopping.BigDecimalAsJsonNumber", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("BigDecimalAsJsonNumberSerializer only supports JSON")
        return BigDecimal(jsonDecoder.decodeJsonElement().jsonPrimitive.content)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("BigDecimalAsJsonNumberSerializer only supports JSON")
        jsonEncoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
    }
}
