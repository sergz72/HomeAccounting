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

@Serializable
data class Account(
    val id: Int,
    val name: String,
    @SerialName("valutaCode") val currency: String,
    @Serializable(with = DBDateSerializer::class) val activeTo: Int?,
    @Serializable(with = IsCashSerializer::class) @SerialName("isCash") var cashAccount: Int?
) {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun fromJson(filePath: Path): Map<Int, Account> {
            filePath.inputStream().use { stream ->
                val accounts = Json.decodeFromStream<Array<Account>>(stream)

                val cashAccounts = accounts.filter { it.cashAccount == null }.map { it.currency to it.id }.toMap()
                val map = accounts.associateBy { it.id }
                accounts.filter { it.cashAccount == 0 }.forEach { it.cashAccount = cashAccounts.getValue(it.currency) }

                return map
            }
        }

        fun fromBinary(buffer: ByteBuffer): Map<Int, Account> {
            var size = buffer.getShort()
            val result = mutableMapOf<Int, Account>()
            while (size-- > 0) {
                val id = buffer.getShort().toInt()
                val account = Account.create(id, buffer)
                result[id] = account
            }
            return result
        }

        private fun create(id: Int, buffer: ByteBuffer): Account {
            val nameSize = buffer.get().toInt()
            var array = ByteArray(nameSize)
            buffer.get(array)
            val name = String(array)
            array = ByteArray(3)
            buffer.get(array)
            val currency = String(array)
            val activeTo = buffer.getInt()
            val cashAccount = buffer.getShort().toInt()
            return Account(id, name, currency, if (activeTo == 0) { null }  else {activeTo},
                            if (cashAccount == 0) { null } else {cashAccount})
        }

        fun toBinary(accounts: Map<Int, Account>, buffer: ByteBuffer) {
            buffer.putShort(accounts.size.toShort())
            for ((id, account) in accounts) {
                buffer.putShort(id.toShort())
                account.toBinary(buffer)
            }
        }
    }

    private fun toBinary(buffer: ByteBuffer) {
        var bytes = name.toByteArray()
        buffer.put(bytes.size.toByte())
        buffer.put(bytes)
        bytes = currency.toByteArray()
        buffer.put(bytes[0])
        buffer.put(bytes[1])
        buffer.put(bytes[2])
        buffer.putInt(activeTo ?: 0)
        buffer.putShort((cashAccount ?: 0).toShort())
    }
}

object IsCashSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IsCash", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int?) {
        TODO()
    }

    override fun deserialize(decoder: Decoder): Int? {
        val isCash = decoder.decodeBoolean()
        return if (isCash) {null} else{0}
    }
}