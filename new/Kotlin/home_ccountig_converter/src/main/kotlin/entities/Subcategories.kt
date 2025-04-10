package com.sz.home_accounting.converter.entities

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.inputStream

enum class SubcategoryCode {
    Comb,
    Comc,
    Fuel,
    Prcn,
    Incc,
    Expc,
    Exch,
    Trfr,
    None
}

enum class SubcategoryOperationCode {
    Incm,
    Expn,
    Spcl
}

@Serializable
data class Subcategory(val id: Int,
                       val name: String,
                       @Serializable(with = CodeSerializer::class)
                       val code: SubcategoryCode,
                       @Serializable(with = OperationCodeSerializer::class)
                       @SerialName("operationCodeId")
                       val operationCode: SubcategoryOperationCode,
                       @SerialName("categoryId")
                       val category: Int) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromJson(filePath: Path): Map<Int, Subcategory> {
            filePath.inputStream().use { stream ->
                val subcategories = Json.decodeFromStream<Array<Subcategory>>(stream)
                return subcategories.associateBy { it.id }
            }
        }

        fun fromBinary(buffer: ByteBuffer): Map<Int, Subcategory> {
            var size = buffer.getShort()
            val result = mutableMapOf<Int, Subcategory>()
            while (size-- > 0) {
                val id = buffer.getShort().toInt()
                val subcategory = create(id, buffer)
                result[id] = subcategory
            }
            return result
        }

        private fun create(id: Int, buffer: ByteBuffer): Subcategory {
            val size = buffer.get().toInt()
            val array = ByteArray(size)
            buffer.get(array)
            val name = String(array)
            val code = SubcategoryCode.entries[buffer.get().toInt()]
            val operationCode = SubcategoryOperationCode.entries[buffer.get().toInt()]
            val category = buffer.getShort().toInt()
            return Subcategory(id, name, code, operationCode, category)
        }

        fun toBinary(subcategories: Map<Int, Subcategory>, buffer: ByteBuffer) {
            buffer.putShort(subcategories.size.toShort())
            for ((id,subcategory) in subcategories) {
                buffer.putShort(id.toShort())
                subcategory.toBinary(buffer)
            }
        }
    }

    private fun toBinary(buffer: ByteBuffer) {
        val bytes = name.toByteArray()
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
        buffer.put(code.ordinal.toByte())
        buffer.put(operationCode.ordinal.toByte())
        buffer.putShort(category.toShort())
    }
}

object CodeSerializer : KSerializer<SubcategoryCode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SubcategoryCode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: SubcategoryCode) {
        TODO()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): SubcategoryCode {
        if (decoder.decodeNotNullMark()) {
            return when (val v = decoder.decodeString()) {
                "COMB" -> SubcategoryCode.Comb
                "COMC" -> SubcategoryCode.Comc
                "INCC" -> SubcategoryCode.Incc
                "EXPC" -> SubcategoryCode.Expc
                "EXCH" -> SubcategoryCode.Exch
                "TRFR" -> SubcategoryCode.Trfr
                "PRCN" -> SubcategoryCode.Prcn
                "FUEL" -> SubcategoryCode.Fuel
                else -> throw IllegalArgumentException("unknown subcategory code $v")
            }
        } else {
            decoder.decodeNull()
            return SubcategoryCode.None
        }
    }
}

object OperationCodeSerializer : KSerializer<SubcategoryOperationCode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SubcategoryOperationCode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: SubcategoryOperationCode) {
        TODO()
    }

    override fun deserialize(decoder: Decoder): SubcategoryOperationCode {
        return when (val v = decoder.decodeString()) {
            "INCM" -> SubcategoryOperationCode.Incm
            "EXPN" -> SubcategoryOperationCode.Expn
            "SPCL" -> SubcategoryOperationCode.Spcl
            else -> throw IllegalArgumentException("unknown subcategory operation code $v")
        }
    }
}
