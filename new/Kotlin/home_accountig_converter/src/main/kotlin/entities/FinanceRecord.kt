package com.sz.home_accounting.converter.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong
import com.sz.home_accounting.converter.DBException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

data class FinanceRecord(var operations: List<FinanceOperation>) {
    companion object {
        fun fromJson(sourceFolder: Path): SortedMap<Int, FinanceRecord> {
            return sourceFolder.listDirectoryEntries()
                .filter { it.isDirectory() }
                .flatMap { it.listDirectoryEntries() }
                .associate { it.parent.name.toInt() to buildFinanceRecord(it) }
                .toSortedMap()
        }

        private fun buildFinanceRecord(filePath: Path): FinanceRecord {
            val contents = Files.readString(filePath)
            var list: List<FinanceOperation>
            try {
                list = Json.decodeFromString<List<FinanceOperation>>(contents)
            } catch (e: Exception) {
                val l = Json.decodeFromString<List<FinanceOperation2>>(contents)
                list = l.map { it.toFinanceOperation() }
            }
            return FinanceRecord(list)
        }
    }

    var totals = mapOf<Int, Long>()

    fun updateChanges(changes: FinanceChanges, dicts: Dicts) {
        operations.forEach { it.updateChanges(changes, dicts) }
    }

    fun buildChanges(dicts: Dicts): FinanceChanges {
        val changes = FinanceChanges(totals)
        operations.forEach {  it.updateChanges(changes, dicts) }
        return changes
    }

    fun toBinary(): ByteArray {
        val buffer = ByteBuffer.allocate(10 * 1024).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(totals.size.toShort())
        for ((accountId, value) in totals) {
            buffer.putShort(accountId.toShort())
            buffer.putLong(value)
        }
        buffer.putShort(operations.size.toShort())
        for (operation in operations) {
            operation.toBinary(buffer)
        }
        return buffer.array().copyOfRange(0, buffer.position());
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class FinanceOperation2(
    val amount: Double?,
    val summa: Double,
    @SerialName("subcategoryId") val subcategory: Int,
    @SerialName("accountId") val account: Int,
    @SerialName("finOpProperies") val properties: List<FinOpProperty2>?
) {
    fun toFinanceOperation(): FinanceOperation {
        return FinanceOperation(
            if (amount == null) null else (amount * 1000).roundToLong(),
            (summa * 100).roundToLong(),
            subcategory,
            account,
            properties?.map { it.toFinOpProperty() }?.toList()
        )
    }
}

@Serializable
data class FinOpProperty2(
    val numericValue: Long?,
    val stringValue: String?,
    @Serializable(with = DBDateSerializer::class) val dateValue: Int?,
    @Serializable(with = PropertyCodeSerializer::class) @SerialName("propertyCode") val code: FinOpPropertyCode
) {
    fun toFinOpProperty(): FinOpProperty {
        return FinOpProperty(numericValue, stringValue, dateValue, code)
    }
}


@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class FinanceOperation(
    @SerialName("Amount") val amount: Long?,
    @SerialName("Summa") val summa: Long,
    @SerialName("SubcategoryId") val subcategory: Int,
    @SerialName("AccountId") val account: Int,
    @SerialName("FinOpProperies") val properties: List<FinOpProperty>?
) {
    fun updateChanges(changes: FinanceChanges, dicts: Dicts) {
        val subcategory = dicts.subcategories.getValue(subcategory)
        when (subcategory.operationCode) {
            SubcategoryOperationCode.Incm -> changes.income(account, summa)
            SubcategoryOperationCode.Expn -> changes.expenditure(account, summa)
            SubcategoryOperationCode.Spcl -> {
                when (subcategory.code) {
                    // Пополнение карточного счета наличными
                    SubcategoryCode.Incc -> handleIncc(changes, dicts.accounts)
                    // Снятие наличных в банкомате
                    SubcategoryCode.Expc -> handleExpc(changes, dicts.accounts)
                    // Обмен валюты
                    SubcategoryCode.Exch -> handleExch(changes)
                    // Перевод средств между платежными картами
                    SubcategoryCode.Trfr -> handleTrfr(changes)
                    else -> throw DBException("unknown subcategory code")
                }
            }
        }
    }

    private fun handleTrfr(changes: FinanceChanges) {
        handleTrfrWithSumma(changes, summa)
    }

    private fun handleExch(changes: FinanceChanges) {
        if (amount != null) {
            handleTrfrWithSumma(changes, amount / 10)
        }
    }

    private fun handleTrfrWithSumma(changes: FinanceChanges, value: Long)
    {
        if (properties == null) return
        changes.expenditure(account, value)
        val secondAccountProperty = properties.find { it.code == FinOpPropertyCode.SECA }
        if (secondAccountProperty?.numericValue != null)
        {
            changes.income(secondAccountProperty.numericValue.toInt(), summa)
        }
    }

    private fun handleExpc(changes: FinanceChanges, accounts: Map<Int, Account>) {
        changes.expenditure(account, summa)
        // cash account for corresponding currency code
        val cashAccount = accounts.getValue(account).cashAccount
        if (cashAccount != null) {
            changes.income(cashAccount, summa)
        }
    }

    private fun handleIncc(changes: FinanceChanges, accounts: Map<Int, Account>) {
        changes.income(account, summa)
        // cash account for corresponding currency code
        val cashAccount = accounts.getValue(account).cashAccount
        if (cashAccount != null) {
            changes.expenditure(cashAccount, summa)
        }
    }

    fun toBinary(buffer: ByteBuffer) {
        buffer.putLong(amount ?: 0)
        buffer.putLong(summa)
        buffer.putShort(subcategory.toShort())
        buffer.putShort(account.toShort())
        if (properties != null) {
            buffer.put(properties.size.toByte())
            for (property in properties) {
                property.toBinary(buffer)
            }
        } else {
            buffer.put(0)
        }
    }
}

enum class FinOpPropertyCode {
    SECA,
    NETW,
    DIST,
    TYPE,
    PPTO,
    AMOU
}

@Serializable
data class FinOpProperty(
    @SerialName("NumericValue") val numericValue: Long?, @SerialName("StringValue") val stringValue: String?,
    @Serializable(with = DBDateSerializer::class) @SerialName("DateValue") val dateValue: Int?,
    @Serializable(with = PropertyCodeSerializer::class) @SerialName("PropertyCode") val code: FinOpPropertyCode
) {
    fun toBinary(buffer: ByteBuffer) {
        buffer.put(code.ordinal.toByte())
        when (code) {
            FinOpPropertyCode.SECA, FinOpPropertyCode.DIST, FinOpPropertyCode.PPTO, FinOpPropertyCode.AMOU -> buffer.putLong(numericValue!!)
            else -> {
                val bytes = stringValue!!.toByteArray(Charsets.UTF_8)
                buffer.put(bytes.size.toByte())
                buffer.put(bytes)
            }
        }
    }
}

object PropertyCodeSerializer : KSerializer<FinOpPropertyCode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FinOpPropertyCode", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: FinOpPropertyCode) {
        TODO()
    }

    override fun deserialize(decoder: Decoder): FinOpPropertyCode {
        return when (val v = decoder.decodeString()) {
            "SECA" -> FinOpPropertyCode.SECA
            "NETW" -> FinOpPropertyCode.NETW
            "DIST" -> FinOpPropertyCode.DIST
            "TYPE" -> FinOpPropertyCode.TYPE
            "PPTO" -> FinOpPropertyCode.PPTO
            "AMOU" -> FinOpPropertyCode.AMOU
            else -> throw IllegalArgumentException("unknown operation property code $v")
        }
    }
}

class FinanceChanges(totals: Map<Int, Long>) {
    val changes: MutableMap<Int, FinanceChange> =
        totals.map { it.key to FinanceChange(it.value, 0, 0) }.toMap().toMutableMap()

    fun buildTotals(): Map<Int, Long> {
        return changes.map { it.key to it.value.getEndSumma() }.toMap()
    }

    fun income(account: Int, value: Long) {
        changes.getOrPut(account) { FinanceChange(0, 0, 0) }.income += value
    }

    fun expenditure(account: Int, value: Long) {
        changes.getOrPut(account) { FinanceChange(0, 0, 0) }.expenditure += value
    }
}

data class FinanceChange(val summa: Long, var income: Long, var expenditure: Long) {
    fun getEndSumma(): Long {
        return summa + income - expenditure
    }
}