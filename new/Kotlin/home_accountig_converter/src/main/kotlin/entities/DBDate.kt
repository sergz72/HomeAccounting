package com.sz.home_accounting.converter.entities

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DBDateSerializer : KSerializer<Int?> {
    private val serializer = ListSerializer(Int.serializer())
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int?) {
        TODO()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Int? {
        if (decoder.decodeNotNullMark()) {
            val values = decoder.decodeSerializableValue(serializer)
            return values[0] * 10000 + values[1] * 100 + values[2]
        }
        return null
    }
}